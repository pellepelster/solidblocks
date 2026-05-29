package de.solidblocks.cloud.services

import de.solidblocks.cloud.Constants.cloudLabels
import de.solidblocks.cloud.Constants.serverName
import de.solidblocks.cloud.Constants.sshHostPrivateKeySecretPath
import de.solidblocks.cloud.Constants.volumeLabels
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.provisioner.context.ProvisionerContext
import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import de.solidblocks.cloud.provisioner.context.ValidationContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.HetznerVolume
import de.solidblocks.cloud.provisioner.secret.GenericSecret
import de.solidblocks.cloud.provisioner.secret.GenericSecretRuntime
import de.solidblocks.cloud.provisioner.secret.OneTimeGeneratedSecret
import de.solidblocks.cloud.utils.ByteSize
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.ssh.KeyType
import de.solidblocks.ssh.SSHKeyUtils
import de.solidblocks.ssh.toPem
import de.solidblocks.utils.LogContext
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

    fun infoJson(cloud: CloudConfigurationRuntime, runtime: R, context: SSHProvisionerContext): Result<ServiceInfo>

    val supportedConfiguration: KClass<C>

    val supportedRuntime: KClass<R>
}

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

data class ServerSSHIdentityResources(val rsaSecret: GenericSecret<GenericSecretRuntime>, val ed25519Secret: GenericSecret<GenericSecretRuntime>) {
    fun list() = listOf(rsaSecret, ed25519Secret)
}

fun createDefaultSSHIdentity(cloud: CloudConfigurationRuntime, runtime: ServiceConfigurationRuntime, index: Int): ServerSSHIdentityResources {
    val serverName = serverName(cloud.environment, runtime.name, index)

    val sshIdentityRsaSecret = GenericSecret<GenericSecretRuntime>(
        sshHostPrivateKeySecretPath(cloud.environment, serverName, KeyType.rsa),
        OneTimeGeneratedSecret {
            val keyPair = SSHKeyUtils.RSA.generate()
            // TODO for some reason ssh-keygen -yf refuses to derive public key from private openssh RSA key
            keyPair.toPem().privateKey
        },
    )

    val sshIdentityED25519Secret = GenericSecret<GenericSecretRuntime>(
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
