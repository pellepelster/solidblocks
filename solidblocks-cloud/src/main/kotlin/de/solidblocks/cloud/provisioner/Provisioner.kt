package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.Output
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffStatus.*
import de.solidblocks.cloud.api.ResourceGroup
import de.solidblocks.cloud.api.endpoint.EndpointProtocol
import de.solidblocks.cloud.api.hierarchicalResourceList
import de.solidblocks.cloud.api.logText
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.Waiter
import de.solidblocks.cloud.utils.Waiter.Companion.defaultWaiter
import de.solidblocks.ssh.SSHClient
import de.solidblocks.utils.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class Provisioner(
    val registry: ProvisionersRegistry,
    val endpointWaiter: Waiter = defaultWaiter(),
) {

    private val logger = KotlinLogging.logger {}

    suspend fun help(resourceGroups: List<ResourceGroup>, context: ProvisionerContext): Result<List<Output>> {
        val result = mutableListOf<Output>()

        for (resourceGroup in resourceGroups) {
            val resources = resourceGroup.hierarchicalResourceList().toSet()

            for (resource in resources) {
                try {
                    val help = registry.help<BaseResource>(resource, context)
                    result.addAll(help)

                } catch (e: Exception) {
                    logger.error(e) { "error creating help for ${resource.logText()}" }
                }
            }
        }

        return Success(result)
    }


    suspend fun diff(
        resourceGroups: List<ResourceGroup>,
        context: ProvisionerContext,
        log: LogContext,
    ): Result<Map<ResourceGroup, List<ResourceDiff>>> {
        val resourceGroupDiffs = resourceGroups.map { resourceGroup ->
            val resourceGroupLogContext = log.indent()

            logInfo(
                "planning changes for resource group ${resourceGroup.name}",
                context = resourceGroupLogContext,
            )
            val diffLogContext = resourceGroupLogContext.indent()

            val diffs = when (val result = diff(resourceGroup, context, diffLogContext)) {
                is Error<List<ResourceDiff>> -> return Error(result.error)
                is Success<List<ResourceDiff>> -> result.data
            }

            diffs.forEach {
                when (it.status) {
                    unknown -> logWarning("could not determine status for ${it.resource.logText()}", context = diffLogContext)

                    missing -> logInfo(bold("will create ${it.resource.logText()}"), context = diffLogContext)

                    up_to_date -> logInfo(dim("${it.resource.logText()} is up-to-date"), context = diffLogContext)

                    has_changes -> {
                        if (it.needsRecreate()) {
                            logInfo(bold(
                                "${it.resource.logText()} has breaking changes and needs to be re-created"),
                                context = diffLogContext,
                            )
                            it.changes.forEach {
                                logInfo(bold("- ${it.logText()}"), context = diffLogContext.indent())
                            }

                        } else {
                            logInfo(bold(
                                "${it.resource.logText()} has pending changes"), context = diffLogContext
                            )
                            it.changes.forEach {
                                logInfo(bold("- ${it.logText()}"), context = diffLogContext.indent())
                            }
                        }
                    }

                    parent_missing -> logInfo(
                        "parent resource for ${it.resource.logText()} is missing",
                        context = diffLogContext,
                    )

                    duplicate -> {
                        return Error(
                            it.duplicateErrorMessage ?: "<unknown duplicate error message for ${it.resource.logText()}>",
                        )
                    }
                }
            }

            resourceGroup to diffs
        }.toMap()

        return Success(resourceGroupDiffs)
    }

    private fun diff(
        resourceGroup: ResourceGroup,
        context: ProvisionerContext,
        log: LogContext,
    ): Result<List<ResourceDiff>> = runBlocking {
        logger.info { "creating diff for ${resourceGroup.logText()}" }

        val resources = resourceGroup.hierarchicalResourceList().filterIsInstance<BaseInfrastructureResource<*>>()
        val result = mutableListOf<ResourceDiff>()

        for (resource in resources) {
            logDebug("creating diff for ${resource.logText()}", context = log)
            try {
                logger.info { "creating diff for ${resource.logText()}" }
                val diff = registry.diff<BaseResource>(resource, context) ?: return@runBlocking Error("diff failed for ${resource.logText()} (null)")

                logDebug(
                    "diff status for ${diff.resource.logText()} is '${diff.status}'",
                    context = log,
                )
                result.add(diff)

                logDebug("finished diff for ${resource.logText()}", context = log)
            } catch (e: Exception) {
                logger.error(e) { "diff failed for ${resource.logText()}" }

                val hasMissingParent = resource.dependsOn.any { parent ->
                    result.any { it.resource == parent && (it.status == missing) }
                }

                if (hasMissingParent) {
                    result.add(ResourceDiff(resource, parent_missing))
                } else {
                    if (resource.dependsOn.isEmpty()) {
                        return@runBlocking Error("diff failed for ${resource.logText()} (${e.message})")
                    } else {
                        logWarning("diff failed for ${resource.logText()} (${e.message})", context = log)
                        result.add(ResourceDiff(resource, parent_missing))
                    }
                }
            }
        }

        return@runBlocking Success(result.toList())
    }

    suspend fun apply(
        resources: List<BaseResource>,
        context: ProvisionerContext,
        log: LogContext,
    ): Result<Unit> {
        val success = resources.map { resource ->
            val runtime = try {
                registry.apply<BaseResource, BaseInfrastructureResourceRuntime>(
                    resource,
                    context,
                    log,
                )
            } catch (e: Exception) {
                logger.error(e) { "creating ${resource.logText()} failed" }
                null
            }

            runtime
        }.all { it != null }

        return if (success) {
            Success(Unit)
        } else {
            Error("")
        }
    }

    fun apply(
        resourceGroupDiffs: Map<ResourceGroup, List<ResourceDiff>>,
        context: ProvisionerContext,
        log: LogContext,
    ): Result<Unit> {
        return runBlocking {
            resourceGroupDiffs.map { (resourceGroup, diffs) ->

                logger.info { "rolling out changes for ${resourceGroup.logText()}" }

                for (diffToDestroy in diffs.filter { it.needsRecreate() }) {
                    val resource = diffToDestroy.resource
                    logger.info { "destroying ${resource.logText()}" }
                    logInfo("destroying ${resource.logText()}", context = log)

                    val result = registry.destroy<BaseResource>(resource, context, log)
                    if (!result) {
                        return@runBlocking Error("destroying ${resource.logText()} failed")
                    }
                }

                val duplicatesDiff = diffs.firstOrNull { it.status == duplicate }
                if (duplicatesDiff != null) {
                    return@runBlocking Error<Unit>(
                        duplicatesDiff.duplicateErrorMessage ?: "<unknown error message>",
                    )
                }

                val resourcesToApply = diffs.filter {
                    it.status != up_to_date && it.status != duplicate
                }.map { it.resource }.hierarchicalResourceList().filterIsInstance<BaseInfrastructureResource<*>>()

                for (resource in resourcesToApply) {
                    logInfo("applying ${resource.logText()}", context = log)

                    val applyLog = log.indent()

                    val applyResult = try {
                        registry.apply<BaseResource, BaseInfrastructureResourceRuntime>(
                            resource,
                            context,
                            applyLog,
                        )
                    } catch (e: Exception) {
                        logger.error(e) { "creating ${resource.logText()} failed" }
                        null
                    }

                    val runtime = applyResult?.runtime
                    if (runtime == null) {
                        return@runBlocking Error("creating ${resource.logText()} failed")
                    }

                    runtime.endpoints.forEach {
                        when (it.protocol) {
                            EndpointProtocol.ssh -> {
                                val sshPortOpen = endpointWaiter.waitForCondition {
                                    try {
                                        logInfo(
                                            "waiting for SSH on endpoint '${it.address}:${it.port}'",
                                            context = applyLog,
                                        )
                                        SSHClient(it.address, context.sshKeyPair).command("whoami").exitCode == 0
                                    } catch (e: Exception) {
                                        logger.error(e) {
                                            "error waiting for '${it.protocol}' endpoint ${it.address}:${it.port}"
                                        }
                                        false
                                    }
                                }

                                if (!sshPortOpen) {
                                    return@runBlocking Error<Unit>(
                                        "error waiting for SSH on ${it.address}:${it.port}",
                                    )
                                }

                                val sshClient = SSHClient(it.address, context.sshKeyPair)

                                val cloudInitFinished = endpointWaiter.waitForCondition {
                                    try {
                                        logInfo(
                                            "waiting for cloud-init to finish on '${it.address}:${it.port}'",
                                            context = applyLog,
                                        )
                                        sshClient.command("test -f /var/lib/cloud/instance/boot-finished").exitCode == 0
                                    } catch (e: Exception) {
                                        false
                                    }
                                }

                                if (!cloudInitFinished) {
                                    return@runBlocking Error<Unit>(
                                        "error waiting for cloud-init to finish on ${it.address}:${it.port}",
                                    )
                                }

                                val result = sshClient.command("cat /var/lib/cloud/data/status.json")

                                if (result.exitCode != 0) {
                                    return@runBlocking Error<Unit>(
                                        "error fetching cloud-init result from ${it.address}:${it.port}",
                                    )
                                }

                                val cloudInitResultHasErrors = try {
                                    val json = Json { this.ignoreUnknownKeys = true }

                                    val cloudInitResult: CloudInitResultWrapper? = json.decodeFromString(result.stdOut)
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
