package de.solidblocks.cloud.services

import de.solidblocks.cloud.Constants.cloudLabels
import de.solidblocks.cloud.Constants.serverName
import de.solidblocks.cloud.Constants.sshHostPrivateKeySecretPath
import de.solidblocks.cloud.Constants.volumeLabels
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.endpoint.EndpointProtocol
import de.solidblocks.cloud.api.endpoint.waitForNoSSH
import de.solidblocks.cloud.api.endpoint.waitForSSH
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.provisioner.context.ProvisionerContext
import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import de.solidblocks.cloud.provisioner.context.ValidationContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.HetznerVolume
import de.solidblocks.cloud.provisioner.pass.OneTimeGeneratedSecret
import de.solidblocks.cloud.provisioner.pass.PassSecret
import de.solidblocks.cloud.status.withServerStatus
import de.solidblocks.cloud.utils.ByteSize
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.LONG_WAIT
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.SHORT_WAIT
import de.solidblocks.cloud.utils.Success
import de.solidblocks.ssh.KeyType
import de.solidblocks.ssh.SSHKeyUtils
import de.solidblocks.ssh.toPem
import de.solidblocks.utils.LogContext
import de.solidblocks.utils.bold
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass

interface ServiceManager<C : ServiceConfiguration, R : ServiceConfigurationRuntime> {

    fun linkedEnvironmentVariables(cloud: CloudConfigurationRuntime, runtime: R): List<BaseEnvironmentVariable> = emptyList()

    fun createResources(cloud: CloudConfigurationRuntime, runtime: R, context: ProvisionerContext): List<BaseInfrastructureResource<*>>

    fun cleanupResources(cloud: CloudConfigurationRuntime, runtime: R, context: ProvisionerContext, log: LogContext): Result<Unit> = Success(Unit)

    fun createProvisioners(runtime: R): List<InfrastructureResourceProvisioner<*, *, *>>

    fun validateConfiguration(
        index: Int,
        cloud: CloudConfiguration,
        configuration: C,
        context: ValidationContext,
        log: LogContext,
    ): Result<R>

    fun infoText(cloud: CloudConfigurationRuntime, runtime: R, context: SSHProvisionerContext): Result<String>

    fun status(cloud: CloudConfigurationRuntime, runtime: R, context: SSHProvisionerContext): Result<String>

    fun maintenance(cloud: CloudConfigurationRuntime, runtime: R, context: SSHProvisionerContext, log: LogContext): Result<Unit> = Success(Unit)

    fun infoJson(cloud: CloudConfigurationRuntime, runtime: R, context: SSHProvisionerContext): Result<ServiceInfo>

    val supportedConfiguration: KClass<C>

    val supportedRuntime: KClass<R>
}

@Suppress("UNCHECKED_CAST")
fun <C : ServiceConfiguration, R : ServiceConfigurationRuntime> List<ServiceRegistration<*, *>>.forService(service: C): ServiceManager<C, R> =
    this.single { it.supportedConfiguration == service::class }.createManager() as ServiceManager<C, R>? ?: throw RuntimeException("no service found for '${service::class.qualifiedName}'")

data class DefaultServerResources(val volumes: DefaultServerVolumes, val sshIdentity: ServerSSHIdentityResources) {
    fun list() = volumes.list() + sshIdentity.list()
}

fun ServiceManager<*, *>.createDefaultServerResources(cloud: CloudConfigurationRuntime, runtime: ServiceConfigurationRuntime, index: Int): DefaultServerResources {
    val serverSSHIdentity = createDefaultSSHIdentity(cloud, runtime, index)
    val serverVolumes = createDefaultServerVolumes(cloud, runtime, index)

    return DefaultServerResources(serverVolumes, serverSSHIdentity)
}

data class ServerSSHIdentityResources(val rsaSecret: PassSecret, val ed25519Secret: PassSecret) {
    fun list() = listOf(rsaSecret, ed25519Secret)
}

fun createDefaultSSHIdentity(cloud: CloudConfigurationRuntime, runtime: ServiceConfigurationRuntime, index: Int): ServerSSHIdentityResources {
    val serverName = serverName(cloud.environment, runtime.name, index)

    val sshIdentityRsaSecret = PassSecret(
        sshHostPrivateKeySecretPath(cloud.environment, serverName, KeyType.rsa),
        OneTimeGeneratedSecret {
            val keyPair = SSHKeyUtils.RSA.generate()
            // TODO for some reason ssh-keygen -yf refuses to derive public key from private openssh RSA key
            keyPair.toPem().privateKey
        },
    )

    val sshIdentityED25519Secret = PassSecret(
        sshHostPrivateKeySecretPath(cloud.environment, serverName, KeyType.ed25519),
        OneTimeGeneratedSecret {
            val keyPair = SSHKeyUtils.ED25519.generate()
            SSHKeyUtils.privateKeyToOpenSsh(keyPair.private)
        },
    )

    return ServerSSHIdentityResources(sshIdentityRsaSecret, sshIdentityED25519Secret)
}

data class DefaultServerVolumes(val data: HetznerVolume) {
    fun list() = listOf(data)
}

fun createDefaultServerVolumes(cloud: CloudConfigurationRuntime, runtime: ServiceConfigurationRuntime, index: Int): DefaultServerVolumes {
    val serverName = serverName(cloud.environment, runtime.name, index)

    val dataVolume = HetznerVolume(
        serverName + "-data",
        runtime.instance.locationWithDefault(cloud.hetznerProviderRuntime()),
        ByteSize.fromGigabytes(runtime.instance.volumeSize),
        volumeLabels(runtime) + cloudLabels(cloud.environment),
    )

    return DefaultServerVolumes(dataVolume)
}

fun serverMaintenance(cloud: CloudConfigurationRuntime, runtime: ServiceConfigurationRuntime, context: SSHProvisionerContext, log: LogContext): Result<Unit> {
    log.info("running maintenance for service '${bold(runtime.name)}'")
    val serverName = serverName(cloud.environment, runtime.name, 0)
    return serverMaintenance(serverName, context, log.indent())
}

fun serverMaintenance(serverName: String, context: SSHProvisionerContext, log: LogContext): Result<Unit> {
    log.info("checking server status '${bold(serverName)}'")
    val serverLog = log.indent()

    val server = context.lookup(HetznerServerLookup(serverName))
    if (server == null) {
        return Error<Unit>("server '$serverName' not found")
    }

    val rebootTriggered = context.withServerStatus(serverName) { sshClient, status ->
        if (status.needRestart.currentKernel != status.needRestart.expectedKernel) {
            serverLog.info("kernel was updated to ${bold(status.needRestart.expectedKernel)} from ${bold(status.needRestart.currentKernel)}, triggering reboot")
            serverLog.info("triggering reboot")
            sshClient.command("reboot")
            runBlocking {
                server.endpoints.forEach { endpoint ->
                    when (endpoint.protocol) {
                        EndpointProtocol.ssh -> SHORT_WAIT.waitForNoSSH(endpoint, context.sshKeyPair) {
                            serverLog.info("waiting for server shutdown")
                        }
                    }
                }
            }
            true
        } else {
            serverLog.info("kernel is up-to-date ${bold(status.needRestart.expectedKernel)}")
            false
        }
    }

    return when (rebootTriggered) {
        is Error<Boolean> -> {
            serverLog.error(rebootTriggered.error)
            Error<Unit>(rebootTriggered.error)
        }

        is Success -> runBlocking {
            if (rebootTriggered.data) {
                serverLog.info("waiting for server reboot")
                server.endpoints.forEach { endpoint ->
                    when (endpoint.protocol) {
                        EndpointProtocol.ssh -> LONG_WAIT.waitForSSH(endpoint, context.sshKeyPair, serverLog)
                    }
                }
            }

            Success(Unit)
        }
    }
}
