package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.api.Error
import de.solidblocks.cloud.api.Result
import de.solidblocks.cloud.api.Success
import de.solidblocks.cloud.api.diff.ResourceDiff
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.duplicate
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.api.diff.logText
import de.solidblocks.cloud.api.endpoint.EndpointProtocol
import de.solidblocks.cloud.api.endpoint.waitForSSH
import de.solidblocks.cloud.api.log.LogContext
import de.solidblocks.cloud.api.resources.*
import de.solidblocks.cloud.provisioner.context.*
import de.solidblocks.cloud.services.ServiceRegistration
import de.solidblocks.cloud.utils.LONG_WAIT
import de.solidblocks.cloud.utils.WaitConfig
import de.solidblocks.cloud.utils.waitForCondition
import de.solidblocks.ssh.SSHClient
import de.solidblocks.utils.logError
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class Provisioner(val registry1: ProvisionersRegistry, val serviceRegistrations: List<ServiceRegistration<*, *>>, val waitConfig: WaitConfig = LONG_WAIT) :
    BaseProvisioner<ProvisionerDiffContext, ProvisionerApplyContext, ProvisionerDestroyContext, ProvisionerLookupContext>(registry1) {

    private val logger = KotlinLogging.logger {}

    fun diff(resourceGroups: List<ResourceGroup>, taintCallback: (BaseInfrastructureResource<*>) -> Boolean, context: SSHProvisionerContext, log: LogContext): Result<Map<ResourceGroup, List<ResourceDiff>>> =
        super.diff(resourceGroups, taintCallback, log, {
            ProvisionerDiffContextImpl(context.sshKeyPair, context.sshKeyAbsolutePath, it, context.environment, registry1, serviceRegistrations)
        })

    suspend fun apply(resources: List<BaseInfrastructureResource<*>>, context: ProvisionerApplyContext): Result<Unit> {
        val failures =
            resources.mapNotNull { resource ->
                try {
                    @Suppress("UNCHECKED_CAST")
                    registry.apply(
                        resource as BaseInfrastructureResource<BaseInfrastructureResourceRuntime>,
                        context,
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

    fun apply(resourceGroupDiffs: Map<ResourceGroup, List<ResourceDiff>>, context: ProvisionerApplyContext): Result<Unit> {
        return runBlocking {
            resourceGroupDiffs.map { (resourceGroup, diffs) ->
                logger.info { "rolling out changes for ${resourceGroup.logText()}" }

                for (diffToDestroy in diffs.filter { it.needsRecreate() }) {
                    val resource = diffToDestroy.resource
                    logger.info { "destroying ${resource.logText()}" }
                    context.log.info("destroying ${resource.logText()}")

                    if (registry.lookup(resource.asLookup(), context) != null) {
                        val result = registry.destroy(
                            resource.asLookup(),
                            object : ProvisionerDestroyContext {
                                override val log: LogContext
                                    get() = log

                                override fun <RuntimeType, ResourceLookupType : InfrastructureResourceLookup<RuntimeType>> lookup(lookup: ResourceLookupType): RuntimeType? = context.lookup(lookup)
                            },
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

                val resourcesToApply =
                    diffs
                        .filter { it.status != up_to_date && it.status != duplicate }
                        .map { it.resource }
                        .hierarchicalResourceList()
                        .filterIsInstance<BaseInfrastructureResource<BaseInfrastructureResourceRuntime>>()

                for (resource in resourcesToApply) {
                    context.log.info("applying ${resource.logText()}")
                    val applyLog = context.log.indent()

                    val result =
                        try {
                            registry.apply(
                                resource,
                                context,
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

                    if (runtime is EndpointResourceRuntime) {
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
            }

            return@runBlocking Success(Unit)
        }
    }
}
