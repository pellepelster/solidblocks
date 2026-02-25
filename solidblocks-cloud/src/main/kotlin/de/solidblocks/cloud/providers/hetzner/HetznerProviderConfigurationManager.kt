package de.solidblocks.cloud.providers.hetzner

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.providers.CloudResourceProviderConfigurationManager
import de.solidblocks.cloud.providers.ConfigurationContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnsrecord.HetznerDnsRecordProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.HetznerDnsZoneProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKeyProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.HetznerVolumeProvisioner
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.utils.LogContext
import de.solidblocks.utils.logDebug
import de.solidblocks.utils.logError
import de.solidblocks.utils.logInfo
import kotlinx.coroutines.runBlocking

class HetznerProviderConfigurationManager : CloudResourceProviderConfigurationManager<
        HetznerProviderConfiguration,
        HetznerProviderConfigurationRuntime,
        > {

    override fun validate(
        configuration: HetznerProviderConfiguration,
        log: LogContext,
        context: ConfigurationContext,
    ): Result<HetznerProviderConfigurationRuntime> {
        if (System.getenv("HCLOUD_TOKEN") == null) {
            "environment variable 'HCLOUD_TOKEN' not set".also {
                    logError(it, context = log)
                    return Error<HetznerProviderConfigurationRuntime>(it)
                }
        }

        logInfo("servers default location is '${configuration.defaultLocation}' and default instance is '${configuration.defaultInstanceType}'", context = log)

        try {
            val api = HetznerApi(System.getenv("HCLOUD_TOKEN"))
            runBlocking { api.servers.list() }
            logInfo("provided Hetzner cloud token is valid", context = log)

            return Success(HetznerProviderConfigurationRuntime(System.getenv("HCLOUD_TOKEN"), configuration.defaultLocation, configuration.defaultInstanceType))
        } catch (_: Exception) {
            "provided Hetzner cloud token is not valid".also {
                    logError(it, context = log)
                    return Error<HetznerProviderConfigurationRuntime>(it)
                }
        }
    }

    override fun createLookupProviders(runtime: HetznerProviderConfigurationRuntime) = listOf(
        HetznerDnsZoneProvisioner(runtime.cloudToken),
    ) as List<ResourceLookupProvider<*, *>>

    override fun createProvisioners(runtime: HetznerProviderConfigurationRuntime) = listOf(
        HetznerDnsRecordProvisioner(runtime.cloudToken),
        HetznerServerProvisioner(runtime.cloudToken),
        HetznerVolumeProvisioner(runtime.cloudToken),
        HetznerSSHKeyProvisioner(runtime.cloudToken),
    ) as List<InfrastructureResourceProvisioner<*, *>>

    override val supportedConfiguration = HetznerProviderConfiguration::class
}
