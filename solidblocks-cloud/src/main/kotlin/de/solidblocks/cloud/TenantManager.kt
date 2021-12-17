package de.solidblocks.cloud

import de.solidblocks.cloud.NetworkUtils.nextNetwork
import de.solidblocks.cloud.NetworkUtils.subnetForNetwork
import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.cloud.config.ConfigConstants.networkName
import de.solidblocks.cloud.config.model.TenantConfiguration
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.hetzner.cloud.network.Network
import de.solidblocks.provisioner.hetzner.cloud.network.Subnet
import de.solidblocks.provisioner.hetzner.cloud.ssh.SshKey
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import mu.KotlinLogging

class TenantManager(
    val provisioner: Provisioner,
    val cloudConfigurationManager: CloudConfigurationManager,
    val hetznerCloudApi: HetznerCloudAPI
) {
    private val logger = KotlinLogging.logger {}

    fun destroy(): Boolean {
        return true
    }

    fun bootstrap(cloudName: String, environmentName: String, tenantName: String): Boolean {
        val environment = cloudConfigurationManager.environmentByName(cloudName, environmentName) ?: return false
        val tenant = cloudConfigurationManager.getTenant(tenantName, cloudName, environmentName) ?: return false

        createTenantModel(
            tenant,
            setOf(SshKey(environment.name, environment.sshSecrets.sshPublicKey))
        )

        return provisioner.apply()
    }

    private fun createTenantModel(
        tenant: TenantConfiguration,
        sshKeys: Set<SshKey> = emptySet()
    ) {

        val currentNetworks = hetznerCloudApi.allNetworks.networks.map { it.ipRange }

        val nextNetwork = nextNetwork(currentNetworks.toSet())

        if (nextNetwork == null) {
            throw RuntimeException("could not determine next network CIDR")
        }

        val networkResourceGroup = provisioner.createResourceGroup("network")

        val network = Network(networkName(tenant), nextNetwork)
        networkResourceGroup.addResource(network)

        val subnet = Subnet(subnetForNetwork(nextNetwork), network)
        networkResourceGroup.addResource(subnet)
    }
}