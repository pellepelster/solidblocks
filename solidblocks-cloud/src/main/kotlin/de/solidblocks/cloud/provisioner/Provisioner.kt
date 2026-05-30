package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffStatus.duplicate
import de.solidblocks.cloud.api.ResourceDiffStatus.has_changes
import de.solidblocks.cloud.api.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.ResourceDiffStatus.parent_missing
import de.solidblocks.cloud.api.ResourceDiffStatus.tainted
import de.solidblocks.cloud.api.ResourceDiffStatus.unknown
import de.solidblocks.cloud.api.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.api.ResourceGroup
import de.solidblocks.cloud.api.endpoint.EndpointProtocol
import de.solidblocks.cloud.api.endpoint.waitForSSH
import de.solidblocks.cloud.api.hierarchicalResourceList
import de.solidblocks.cloud.api.logText
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.context.ProvisionerApplyContext
import de.solidblocks.cloud.provisioner.context.ProvisionerDiffContext
import de.solidblocks.cloud.provisioner.context.ProvisionerDiffContextImpl
import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import de.solidblocks.cloud.services.ServiceRegistration
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.LONG_WAIT
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.WaitConfig
import de.solidblocks.cloud.utils.waitForCondition
import de.solidblocks.ssh.SSHClient
import de.solidblocks.utils.LogContext
import de.solidblocks.utils.bold
import de.solidblocks.utils.dim
import de.solidblocks.utils.logError
import de.solidblocks.utils.logWarning
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class Provisioner(val registry: ProvisionersRegistry, val serviceRegistrations: List<ServiceRegistration<*, *>>, val waitConfig: WaitConfig = LONG_WAIT) {

    private val logger = KotlinLogging.logger {}

    fun diff(resourceGroups: List<ResourceGroup>, taintCallback: (BaseInfrastructureResource<*>) -> Boolean, context: SSHProvisionerContext, log: LogContext): Result<Map<ResourceGroup, List<ResourceDiff>>> {
        val changedResources = mutableListOf<BaseInfrastructureResource<*>>()
        val taintedResources = mutableSetOf<BaseInfrastructureResource<*>>()

        val resourceGroupDiffs =
            resourceGroups
                .map { resourceGroup ->
                    val resourceGroupLogContext = log.indent()

                    resourceGroupLogContext.info(bold("planning changes for resource group ${resourceGroup.name}"))
                    val diffLogContext = resourceGroupLogContext.indent()

                    val diffs =
                        when (
                            val result = diff(
                                resourceGroup,
                                ProvisionerDiffContextImpl(context.sshKeyPair, context.sshKeyAbsolutePath, changedResources, context.environment, registry, serviceRegistrations),
                                taintCallback,
                                taintedResources,
                                diffLogContext,
                            )
                        ) {
                            is Error<List<ResourceDiff>> -> return Error(result.error)
                            is Success<List<ResourceDiff>> -> result.data
                        }
                    changedResources.addAll(diffs.filter { it.status != up_to_date }.map { it.resource })

                    diffs.forEach {
                        when (it.status) {
                            unknown ->
                                logWarning(
                                    "could not determine status for ${it.resource.logText()}",
                                    context = diffLogContext,
                                )

                            missing ->
                                diffLogContext.info(bold("will create ${it.resource.logText()}"))

                            up_to_date ->
                                diffLogContext.info(dim("${it.resource.logText()} is up-to-date"))

                            has_changes -> {
                                if (it.needsRecreate()) {
                                    diffLogContext.info(bold("${it.resource.logText()} has breaking changes and needs to be re-created"))
                                } else {
                                    diffLogContext.info(bold("${it.resource.logText()} has pending changes"))
                                }
                                it.changes.forEach {
                                    diffLogContext.indent().info(bold("- ${it.logText()}"))
                                }
                            }

                            parent_missing ->
                                diffLogContext.info("parent resource for ${it.resource.logText()} is missing")

                            duplicate -> {
                                return Error(
                                    it.duplicateErrorMessage
                                        ?: "<unknown duplicate error message for ${it.resource.logText()}>",
                                )
                            }

                            tainted -> {
                                diffLogContext.info(bold("${it.resource.logText()} is tainted"))
                            }
                        }
                    }

                    resourceGroup to diffs
                }
                .toMap()

        return Success(resourceGroupDiffs)
    }

    private fun diff(
        resourceGroup: ResourceGroup,
        context: ProvisionerDiffContext,
        taintCallback: (BaseInfrastructureResource<*>) -> Boolean,
        taintedResources: MutableSet<BaseInfrastructureResource<*>>,
        log: LogContext,
    ): Result<List<ResourceDiff>> = runBlocking {
        logger.info { "creating diff for ${resourceGroup.logText()}" }

        val resources =
            resourceGroup.hierarchicalResourceList().filterIsInstance<BaseInfrastructureResource<*>>()
        val result = mutableListOf<ResourceDiff>()

        val missingResources = mutableSetOf<BaseInfrastructureResource<*>>()
        val resourcesNeedingRecreate = mutableSetOf<BaseInfrastructureResource<*>>()

        for (resource in resources) {
            log.info("creating diff for ${resource.logText()}")
            val diffLog = log.indent()

            if (resource.dependsOnAnyOf(missingResources)) {
                diffLog.debug("parent resource for ${resource.logText()} is missing, skipping diff")
                missingResources.add(resource)
                result.add(ResourceDiff(resource, tainted))
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
                    registry.diff<BaseResource>(resource, context)
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

    private fun BaseInfrastructureResource<*>.dependsOnAnyOf(missing: Set<BaseInfrastructureResource<*>>): Boolean = this.dependsOn.any { dependency ->
        when (dependency) {
            is BaseInfrastructureResource<*> -> dependency in missing
            is InfrastructureResourceLookup<*> -> missing.any { it.lookupType == dependency::class }
            else -> false
        }
    }

    suspend fun apply(resources: List<BaseResource>, context: ProvisionerApplyContext, log: LogContext): Result<Unit> {
        val failures =
            resources.mapNotNull { resource ->
                try {
                    registry.apply<BaseInfrastructureResourceRuntime>(
                        resource,
                        context,
                        log,
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

    fun apply(resourceGroupDiffs: Map<ResourceGroup, List<ResourceDiff>>, context: ProvisionerApplyContext, log: LogContext): Result<Unit> {
        return runBlocking {
            resourceGroupDiffs.map { (resourceGroup, diffs) ->
                logger.info { "rolling out changes for ${resourceGroup.logText()}" }

                for (diffToDestroy in diffs.filter { it.needsRecreate() }) {
                    val resource = diffToDestroy.resource
                    logger.info { "destroying ${resource.logText()}" }
                    log.info("destroying ${resource.logText()}")

                    if (registry.lookup(resource.asLookup(), context) != null) {
                        val result = registry.destroy(resource.asLookup(), context, log)
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

                val resourcesToApply =
                    diffs
                        .filter { it.status != up_to_date && it.status != duplicate }
                        .map { it.resource }
                        .hierarchicalResourceList()
                        .filterIsInstance<BaseInfrastructureResource<*>>()

                for (resource in resourcesToApply) {
                    log.info("applying ${resource.logText()}")
                    val applyLog = log.indent()

                    val result =
                        try {
                            registry.apply(
                                resource,
                                context,
                                applyLog,
                            )
                        } catch (e: Exception) {
                            logger.error(e) { "creating ${resource.logText()} failed" }
                            Error<BaseInfrastructureResourceRuntime>(e.message ?: "<unknown>")
                        }

                    val runtime =
                        when (result) {
                            is Error<BaseInfrastructureResourceRuntime> ->
                                return@runBlocking Error<Unit>(result.error)

                            is Success<BaseInfrastructureResourceRuntime> -> result.data
                        }

                    runtime.endpoints.forEach {
                        when (it.protocol) {
                            EndpointProtocol.ssh -> {
                                val sshPortOpen =
                                    waitConfig.waitForSSH(it, context.sshKeyPair, applyLog)

                                if (!sshPortOpen) {
                                    return@runBlocking Error<Unit>(
                                        "error waiting for SSH on ${it.address}:${it.port}",
                                    )
                                }

                                val cloudInitFinished =
                                    waitConfig.waitForCondition {
                                        try {
                                            val sshClient = SSHClient(it.address, context.sshKeyPair, null, port = it.port)
                                            applyLog.info("waiting for cloud-init to finish on '${it.address}:${it.port}'")
                                            sshClient
                                                .command("test -f /var/lib/cloud/instance/boot-finished")
                                                .exitCode == 0
                                        } catch (e: Exception) {
                                            false
                                        }
                                    }

                                if (!cloudInitFinished) {
                                    return@runBlocking Error<Unit>(
                                        "error waiting for cloud-init to finish on ${it.address}:${it.port}",
                                    )
                                }

                                val sshClient = SSHClient(it.address, context.sshKeyPair, null, port = it.port)
                                val result = sshClient.command("cat /var/lib/cloud/data/status.json")

                                if (result.exitCode != 0) {
                                    return@runBlocking Error<Unit>(
                                        "error fetching cloud-init result from ${it.address}:${it.port}",
                                    )
                                }

                                val cloudInitResultHasErrors =
                                    try {
                                        val json = Json { this.ignoreUnknownKeys = true }

                                        val cloudInitResult: CloudInitResultWrapper? =
                                            json.decodeFromString(result.stdOut)
                                        if (cloudInitResult == null) {
                                            return@runBlocking Error<Unit>(
                                                "error deserializing cloud-init result from ${it.address}:${it.port}",
                                            )
                                        }

                                        cloudInitResult.hasErrors
                                    } catch (e: Exception) {
                                        logger.error(e) { "failed to deserialize cloud-init status" }
                                        logError("failed to deserialize cloud-init status")
                                        false
                                    }

                                if (cloudInitResultHasErrors) {
                                    val cloudInitOutputLog = sshClient.download("/var/log/cloud-init-output.log")

                                    return@runBlocking Error<Unit>(
                                        "cloud-init has errors on ${it.address}:${it.port}, '/var/log/cloud-init-output.log' was:\n---\n${cloudInitOutputLog?.toString(Charsets.UTF_8)}---\n",
                                    )
                                }
                            }
                        }
                    }
                }
            }

            return@runBlocking Success(Unit)
        }
    }
}
