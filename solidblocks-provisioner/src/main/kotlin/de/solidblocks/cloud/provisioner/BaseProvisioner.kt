package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.api.Error
import de.solidblocks.cloud.api.Result
import de.solidblocks.cloud.api.Success
import de.solidblocks.cloud.api.diff.ResourceDiff
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.duplicate
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.has_changes
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.parent_missing
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.tainted
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.unknown
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.api.diff.logText
import de.solidblocks.cloud.api.endpoint.Endpoint
import de.solidblocks.cloud.api.log.LogContext
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.EndpointResourceRuntime
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.api.resources.ResourceGroup
import de.solidblocks.cloud.api.resources.hierarchicalResourceList
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking

abstract class BaseProvisioner<
        DiffContextType,
        ApplyContextType,
        DestroyContextType,
        LookupContextType,
        >(
    val registry: BaseProvisionersRegistry<DiffContextType, ApplyContextType, DestroyContextType, LookupContextType>,
) {

    private val logger = KotlinLogging.logger {}

    fun diff(
        resourceGroups: List<ResourceGroup>,
        taintCallback: (BaseInfrastructureResource<*>) -> Boolean,
        log: LogContext,
        createDiffContext: (MutableList<BaseInfrastructureResource<*>>) -> DiffContextType,
    ): Result<Map<ResourceGroup, List<ResourceDiff>>> {
        val changedResources = mutableListOf<BaseInfrastructureResource<*>>()
        val taintedResources = mutableSetOf<BaseInfrastructureResource<*>>()

        val resourceGroupDiffs = resourceGroups.map { resourceGroup ->
            val resourceGroupLogContext = log.indent()

            resourceGroupLogContext.info(log.bold("planning changes for resource group ${resourceGroup.name}"))
            val diffLogContext = resourceGroupLogContext.indent()

            val diffs = when (val result = diff(
                resourceGroup,
                createDiffContext(changedResources),
                taintCallback,
                taintedResources,
                diffLogContext,
            )) {
                is Error<List<ResourceDiff>> -> return Error(result.error)
                is Success<List<ResourceDiff>> -> result.data
            }
            changedResources.addAll(diffs.filter { it.status != up_to_date }.map { it.resource })

            diffs.forEach {
                when (it.status) {
                    unknown -> log.warning(
                        "could not determine status for ${it.resource.logText()}",
                    )

                    missing -> diffLogContext.info(log.bold("will create ${it.resource.logText()}"))

                    up_to_date -> diffLogContext.info(log.dim("${it.resource.logText()} is up-to-date"))

                    has_changes -> {
                        if (it.needsRecreate()) {
                            diffLogContext.info(log.bold("${it.resource.logText()} has breaking changes and needs to be re-created"))
                        } else {
                            diffLogContext.info(log.bold("${it.resource.logText()} has pending changes"))
                        }
                        it.changes.forEach {
                            diffLogContext.indent().info(log.bold("- ${it.logText()}"))
                        }
                    }

                    parent_missing -> diffLogContext.info("parent resource for ${it.resource.logText()} is missing")

                    duplicate -> {
                        return Error(
                            it.duplicateErrorMessage ?: "<unknown duplicate error message for ${it.resource.logText()}>",
                        )
                    }

                    tainted -> {
                        diffLogContext.info(log.bold("${it.resource.logText()} is tainted"))
                    }
                }
            }

            resourceGroup to diffs
        }.toMap()

        return Success(resourceGroupDiffs)
    }

    private fun diff(
        resourceGroup: ResourceGroup,
        context: DiffContextType,
        taintCallback: (BaseInfrastructureResource<*>) -> Boolean,
        taintedResources: MutableSet<BaseInfrastructureResource<*>>,
        log: LogContext,
    ): Result<List<ResourceDiff>> = runBlocking {
        logger.info { "creating diff for ${resourceGroup.logText()}" }

        val resources = resourceGroup.hierarchicalResourceList().filterIsInstance<BaseInfrastructureResource<*>>()
        val result = mutableListOf<ResourceDiff>()

        val missingResources = mutableSetOf<BaseInfrastructureResource<*>>()
        val resourcesNeedingRecreate = mutableSetOf<BaseInfrastructureResource<*>>()

        for (resource in resources) {
            log.info("creating diff for ${resource.logText()}")
            val diffLog = log.indent()

            if (resource.dependsOnAnyOf(missingResources)) {
                diffLog.debug("parent resource for ${resource.logText()} is missing, skipping diff")
                missingResources.add(resource)
                result.add(ResourceDiff(resource, missing))
                continue
            }

            val isAnyParentTainted = resource.recursiveDependsOn().filterIsInstance<BaseInfrastructureResource<*>>().any { it in taintedResources }

            val diff = if (isAnyParentTainted || taintCallback.invoke(resource)) {
                if (resource.taintable) {
                    taintedResources.add(resource)
                    ResourceDiff(resource, tainted)
                } else {
                    log.warning("${resource.logText()} could not be tainted, manual intervention might be needed")
                    ResourceDiff(resource, up_to_date)
                }
            } else {
                val diffResult = try {
                    registry.diff(resource, context)
                } catch (e: Exception) {
                    logger.error(e) { "diff failed for ${resource.logText()}" }
                    return@runBlocking Error<List<ResourceDiff>>("diff failed for ${resource.logText()} (${e.message})", e)
                }
                when (diffResult) {
                    is Error<ResourceDiff> -> return@runBlocking Error(diffResult.error, diffResult.cause)
                    is Success<ResourceDiff> -> diffResult.data
                }
            }

            if (diff.status == missing || diff.status == parent_missing) {
                missingResources.add(resource)
            }

            if (diff.status == has_changes || diff.needsRecreate()) {
                resourcesNeedingRecreate.add(resource)
            }

            diffLog.debug("diff status for ${diff.resource.logText()} is '${diff.status}' (needsRecreate: ${diff.needsRecreate()})")
            result.add(diff)
            diffLog.debug("finished diff for ${resource.logText()}")
        }

        return@runBlocking Success(result.toList())
    }

    suspend fun apply(
        resources: List<BaseInfrastructureResource<*>>, createApplyContext: () -> ApplyContextType
    ): Result<Unit> {

        val failures = resources.mapNotNull { resource ->
            try {
                @Suppress("UNCHECKED_CAST") registry.apply(
                    resource as BaseInfrastructureResource<BaseInfrastructureResourceRuntime>,
                    createApplyContext(),
                )
                null
            } catch (e: Exception) {
                logger.error(e) { "creating ${resource.logText()} failed" }
                resource.logText()
            }
        }

        return if (failures.isEmpty()) {
            Success(Unit)
        } else {
            Error("failed to apply ${failures.size} resource(s): ${failures.joinToString(", ")}")
        }
    }

    fun apply(
        resourceGroupDiffs: Map<ResourceGroup, List<ResourceDiff>>,
        createLookupContext: () -> LookupContextType,
        createApplyContext: () -> ApplyContextType,
        createDestroyContext: () -> DestroyContextType, log: LogContext
    ): Result<Unit> {
        val applyContext = createApplyContext()

        return runBlocking {
            resourceGroupDiffs.map { (resourceGroup, diffs) ->
                logger.info { "rolling out changes for ${resourceGroup.logText()}" }

                for (diffToDestroy in diffs.filter { it.needsRecreate() }) {
                    val resource = diffToDestroy.resource
                    logger.info { "destroying ${resource.logText()}" }
                    log.info("destroying ${resource.logText()}")

                    if (registry.lookup(resource.asLookup(), createLookupContext()) != null) {
                        val result = registry.destroy(
                            resource.asLookup(),
                            createDestroyContext(),
                        )
                        if (!result) {
                            return@runBlocking Error("destroying ${resource.logText()} failed")
                        }
                    }
                }

                val duplicatesDiff = diffs.firstOrNull { it.status == duplicate }
                if (duplicatesDiff != null) {
                    return@runBlocking Error<Unit>(
                        duplicatesDiff.duplicateErrorMessage ?: "<unknown error message>",
                    )
                }

                val resourcesToApply = diffs.filter { it.status != up_to_date && it.status != duplicate }.map { it.resource }.hierarchicalResourceList()
                    .filterIsInstance<BaseInfrastructureResource<BaseInfrastructureResourceRuntime>>()


                for (resource in resourcesToApply) {
                    log.info("applying ${resource.logText()}")
                    val applyLog = log.indent()

                    val result = try {
                        registry.apply(
                            resource,
                            applyContext,
                        )
                    } catch (e: Exception) {
                        logger.error(e) { "creating ${resource.logText()} failed" }
                        Error<BaseInfrastructureResourceRuntime>(e.message ?: "<unknown>")
                    }

                    val runtime = when (result) {
                        is Error<BaseInfrastructureResourceRuntime> -> return@runBlocking Error<Unit>(result.error)

                        is Success<BaseInfrastructureResourceRuntime> -> result.data
                    }

                    if (runtime is EndpointResourceRuntime) {
                        runtime.endpoints.forEach {
                            when (val result = waitForEndpoint(it, applyContext, applyLog)) {
                                is Error<Unit> -> return@runBlocking result
                                is Success<*> -> {}
                            }
                        }
                    }
                }
            }

            return@runBlocking Success(Unit)
        }
    }

    abstract suspend fun waitForEndpoint(it: Endpoint, context: ApplyContextType, log: LogContext): Result<Unit>
}

fun BaseInfrastructureResource<*>.dependsOnAnyOf(missing: Set<BaseInfrastructureResource<*>>): Boolean = this.dependsOn.any { dependency ->
    when (dependency) {
        is BaseInfrastructureResource<*> -> dependency in missing
        is InfrastructureResourceLookup<*> -> missing.any { it.lookupType == dependency::class }
        else -> false
    }
}
