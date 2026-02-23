package de.solidblocks.cloud

import de.solidblocks.cloud.Constants.sshKeyName
import de.solidblocks.cloud.api.InfrastructureResourceHelp
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceGroup
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.providers.*
import de.solidblocks.cloud.providers.ssh.SSHKeyProviderRuntime
import de.solidblocks.cloud.provisioner.Provisioner
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.provisioner.ProvisionersRegistry
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKey
import de.solidblocks.cloud.provisioner.userdata.UserDataLookupProvider
import de.solidblocks.cloud.services.*
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.ssh.SSHKeyUtils
import de.solidblocks.utils.LogContext
import de.solidblocks.utils.bold
import de.solidblocks.utils.logInfo
import kotlinx.coroutines.runBlocking

class CloudProvisioner(
    val runtime: CloudManager.CloudRuntime,
    val serviceRegistrations: List<ServiceRegistration<*, *>>,
    val providerRegistrations: List<ProviderRegistration<*, *, *>>,
) {
    val sshRuntime = runtime.providers.filterIsInstance<SSHKeyProviderRuntime>().single()

    val registry = createRegistry()

    val provisionerContext =
        ProvisionerContext(
            sshRuntime.keyPair,
            runtime.configuration.name,
            runtime.configuration.getDefaultEnvironment(),
            registry,
        )

    fun plan(log: LogContext): Result<Map<ResourceGroup, List<ResourceDiff>>> = runBlocking {
        val provisioner = createProvisioner()
        logInfo(bold("planning changes for cloud configuration '${runtime.configuration.name}'"))
        val resourceGroups = createResourceGroups()
        return@runBlocking provisioner.diff(resourceGroups, provisionerContext, log)
    }

    fun help(log: LogContext): Result<List<InfrastructureResourceHelp>> = runBlocking {
        val provisioner = createProvisioner()
        val resourceGroups = createResourceGroups()

        return@runBlocking provisioner.help(resourceGroups, provisionerContext, log)
    }

    fun apply(log: LogContext): Result<Unit> = runBlocking {
        val provisioner = createProvisioner()

        val diffs =
            when (val result = plan(log)) {
                is Error<Map<ResourceGroup, List<ResourceDiff>>> ->
                    return@runBlocking Error<Unit>(result.error)

                is Success<Map<ResourceGroup, List<ResourceDiff>>> -> result.data
            }

        logInfo(bold("rolling out changes for cloud configuration '${runtime.configuration.name}'"))
        return@runBlocking provisioner.apply(diffs, provisionerContext, log.indent())
    }

    private fun createResourceGroups(): List<ResourceGroup> {
        val sshKeyRuntime = runtime.providers.filterIsInstance<SSHKeyProviderRuntime>().single()

        val publicKey = SSHKeyUtils.publicKeyToOpenSSH(sshKeyRuntime.keyPair.public)
        val sshKey = HetznerSSHKey(sshKeyName(runtime.configuration), publicKey, emptyMap())

        val cloudResourceGroup = ResourceGroup("cloud '${runtime.configuration.name}'", listOf(sshKey))

        val serviceResourceGroups =
            runtime.services.map {
                val manager:
                        ServiceConfigurationManager<ServiceConfiguration, ServiceConfigurationRuntime> =
                    serviceRegistrations.managerForService(it, runtime.configuration)

                ResourceGroup(
                    "service '${it.name}'",
                    manager.createResources(it),
                    setOf(cloudResourceGroup),
                )
            }
        return listOf(cloudResourceGroup) + serviceResourceGroups
    }

    private fun createProvisioner(): Provisioner {
        val provisioner = Provisioner(registry)
        return provisioner
    }

    private fun createRegistry(): ProvisionersRegistry {
        val configurationProvisioners =
            runtime.providers.flatMap {
                val manager: ProviderConfigurationManager<ProviderConfiguration, ProviderRuntime> =
                    providerRegistrations.managerForRuntime(it)
                manager.createProvisioners(it)
            }

        val configurationLookups =
            runtime.providers.flatMap {
                val manager: ProviderConfigurationManager<ProviderConfiguration, ProviderRuntime> =
                    providerRegistrations.managerForRuntime(it)
                manager.createLookupProviders(it)
            }

        val serviceProvisioners =
            runtime.services.flatMap {
                val manager:
                        ServiceConfigurationManager<ServiceConfiguration, ServiceConfigurationRuntime> =
                    serviceRegistrations.managerForService(it, runtime.configuration)
                manager.createProvisioners(it)
            }

        val lookups =
            configurationLookups +
                    listOf(UserDataLookupProvider()) +
                    (configurationProvisioners + serviceProvisioners).filterIsInstance<
                            ResourceLookupProvider<*, *>,
                            >()
        return ProvisionersRegistry(lookups, configurationProvisioners + serviceProvisioners)
    }
}
