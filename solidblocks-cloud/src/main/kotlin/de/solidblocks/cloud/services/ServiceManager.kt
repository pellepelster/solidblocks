package de.solidblocks.cloud.services

import de.solidblocks.cloud.Constants
import de.solidblocks.cloud.Constants.cloudLabels
import de.solidblocks.cloud.Constants.serverName
import de.solidblocks.cloud.Constants.sshHostPrivateKeySecretPath
import de.solidblocks.cloud.Constants.volumeLabels
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.provisioner.CloudProvisionerContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.HetznerVolume
import de.solidblocks.cloud.provisioner.pass.OneTimeGeneratedSecret
import de.solidblocks.cloud.provisioner.pass.PassSecret
import de.solidblocks.cloud.utils.ByteSize
import de.solidblocks.cloud.utils.Result
import de.solidblocks.ssh.SSHKeyUtils
import de.solidblocks.utils.LogContext
import kotlin.reflect.KClass

interface ServiceManager<C : ServiceConfiguration, R : ServiceConfigurationRuntime> {

    fun linkedEnvironmentVariables(cloud: CloudConfigurationRuntime, runtime: R): List<BaseEnvironmentVariable> = emptyList()

    fun createResources(cloud: CloudConfigurationRuntime, runtime: R, context: CloudProvisionerContext): List<BaseInfrastructureResource<*>>

    fun createProvisioners(runtime: R): List<InfrastructureResourceProvisioner<*, *>>

    fun validateConfiguration(
        index: Int,
        cloud: CloudConfiguration,
        configuration: C,
        context: CloudProvisionerContext,
        log: LogContext,
    ): Result<R>

    fun infoText(cloud: CloudConfigurationRuntime, runtime: R, context: CloudProvisionerContext): Result<String>

    fun status(cloud: CloudConfigurationRuntime, runtime: R, context: CloudProvisionerContext): Result<String>

    fun infoJson(cloud: CloudConfigurationRuntime, runtime: R, context: CloudProvisionerContext): Result<ServiceInfo>

    val supportedConfiguration: KClass<C>

    val supportedRuntime: KClass<R>
}

fun <C : ServiceConfiguration, R : ServiceConfigurationRuntime> List<ServiceRegistration<*, *>>.forService(service: C): ServiceManager<C, R> =
    this.single { it.supportedConfiguration == service::class }.createManager()
        as ServiceManager<C, R>?
        ?: throw RuntimeException("no service found for '${service::class.qualifiedName}'")

data class DefaultServerResources(val dataVolume: HetznerVolume, val sshIdentityRsaSecret: PassSecret, val sshIdentityED25519Secret: PassSecret) {
    fun list() = listOf(dataVolume, sshIdentityRsaSecret, sshIdentityED25519Secret)
}

fun ServiceManager<*, *>.createDefaultResources(cloud: CloudConfigurationRuntime, runtime: ServiceConfigurationRuntime): DefaultServerResources {
    val serverName = serverName(cloud.environment, runtime.name)

    val sshIdentityRsaSecret = PassSecret(
        sshHostPrivateKeySecretPath(cloud.environment, serverName, Constants.SshHostKeyType.rsa),
        OneTimeGeneratedSecret {
            val keyPair = SSHKeyUtils.RSA.generate()
            SSHKeyUtils.privateKeyToOpenSsh(keyPair.private)
        },
    )

    val sshIdentityED25519Secret = PassSecret(
        sshHostPrivateKeySecretPath(cloud.environment, serverName, Constants.SshHostKeyType.ed25519),
        OneTimeGeneratedSecret {
            val keyPair = SSHKeyUtils.ED25519.generate()
            SSHKeyUtils.privateKeyToOpenSsh(keyPair.private)
        },
    )

    val dataVolume = HetznerVolume(
        serverName + "-data",
        runtime.instance.locationWithDefault(cloud.hetznerProviderRuntime()),
        ByteSize.fromGigabytes(runtime.instance.volumeSize),
        volumeLabels(runtime) + cloudLabels(cloud.environment),
    )

    return DefaultServerResources(dataVolume, sshIdentityRsaSecret, sshIdentityED25519Secret)
}
