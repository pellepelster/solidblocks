package de.solidblocks.cloud.providers.hetzner

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.providers.CloudConfigurationContext
import de.solidblocks.cloud.providers.CloudResourceProviderConfigurationManager
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnsrecord.HetznerDnsRecordProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.HetznerDnsZoneProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.firewall.HetznerFirewallProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerNetworkProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerSubnetProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKeyProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.HetznerVolumeProvisioner
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking

class HetznerProviderConfigurationManager :
    CloudResourceProviderConfigurationManager<HetznerProviderConfiguration, HetznerProviderRuntime> {

    private val logger = KotlinLogging.logger {}

    override fun validate(configuration: HetznerProviderConfiguration, context: CloudConfigurationContext, log: LogContext): Result<HetznerProviderRuntime> {
        if (System.getenv("HCLOUD_TOKEN") == null) {
            "environment variable 'HCLOUD_TOKEN' not set"
                .also {
                    log.error(it)
                    return Error<HetznerProviderRuntime>(it)
                }
        }

        log.info("servers default location is '${configuration.defaultLocation}' and default instance is '${configuration.defaultInstanceType}'")

        try {
            val api = HetznerApi(System.getenv("HCLOUD_TOKEN"))
            runBlocking { api.servers.list() }
            log.info("provided Hetzner cloud token is valid")

            return Success(
                HetznerProviderRuntime(
                    System.getenv("HCLOUD_TOKEN"),
                    configuration.defaultLocation,
                    configuration.defaultInstanceType,
                ),
            )
        } catch (e: Exception) {
            "provided Hetzner cloud token is not valid"
                .also {
                    logger.error(e) { it }
                    log.error(it)
                    return Error<HetznerProviderRuntime>(it)
                }
        }
    }

    override fun createLookupProviders(runtime: HetznerProviderRuntime) = listOf(HetznerDnsZoneProvisioner(runtime.cloudToken)) as List<ResourceLookupProvider<*, *>>

    override fun createProvisioners(runtime: HetznerProviderRuntime) = listOf(
        HetznerDnsRecordProvisioner(runtime.cloudToken),
        HetznerServerProvisioner(runtime.cloudToken),
        HetznerVolumeProvisioner(runtime.cloudToken),
        HetznerSSHKeyProvisioner(runtime.cloudToken),
        HetznerNetworkProvisioner(runtime.cloudToken),
        HetznerSubnetProvisioner(runtime.cloudToken),
        HetznerFirewallProvisioner(runtime.cloudToken),
    )
        as List<InfrastructureResourceProvisioner<*, *>>

    override val supportedConfiguration = HetznerProviderConfiguration::class
}
