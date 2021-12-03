package de.solidblocks.cloud

import de.solidblocks.api.resources.infrastructure.network.Network
import de.solidblocks.api.resources.infrastructure.network.Subnet
import de.solidblocks.api.resources.infrastructure.ssh.SshKey
import de.solidblocks.cloud.NetworkUtils.nextNetwork
import de.solidblocks.cloud.NetworkUtils.subnetForNetwork
import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.cloud.config.model.TenantConfiguration
import de.solidblocks.provisioner.Provisioner
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class TenantMananger(
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

        val network = Network(tenant.name, nextNetwork)
        networkResourceGroup.addResource(network)

        val subnet = Subnet(subnetForNetwork(nextNetwork), network)
        networkResourceGroup.addResource(subnet)

    }
}
