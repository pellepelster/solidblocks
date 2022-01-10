package de.solidblocks.cloud

import de.solidblocks.base.TenantReference
import de.solidblocks.cloud.NetworkUtils.nextNetwork
import de.solidblocks.cloud.NetworkUtils.subnetForNetwork
import de.solidblocks.cloud.model.EnvironmentRepository
import de.solidblocks.cloud.model.ModelConstants.defaultLabels
import de.solidblocks.cloud.model.ModelConstants.networkName
import de.solidblocks.cloud.model.TenantRepository
import de.solidblocks.cloud.model.entities.TenantEntity
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.hetzner.cloud.network.Network
import de.solidblocks.provisioner.hetzner.cloud.network.Subnet
import de.solidblocks.provisioner.hetzner.cloud.ssh.SshKey
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import mu.KotlinLogging

class TenantProvisioner(
    private val reference: TenantReference,
    val provisioner: Provisioner,
    val environmentRepository: EnvironmentRepository,
    val tenantRepository: TenantRepository,
    val hetznerCloudApi: HetznerCloudAPI
) {
    private val logger = KotlinLogging.logger {}

    fun destroy(): Boolean {
        return true
    }

    fun bootstrap(): Boolean {
        val environment = environmentRepository.getEnvironment(reference)
        val tenant = tenantRepository.getTenant(reference)

        createTenantModel(
            tenant,
            setOf(SshKey(environment.name, environment.sshSecrets.sshPublicKey))
        )

        return provisioner.apply()
    }

    private fun createTenantModel(
        tenant: TenantEntity,
        sshKeys: Set<SshKey> = emptySet()
    ) {

        val currentNetworks = hetznerCloudApi.allNetworks.networks.map { it.ipRange }

        val nextNetwork = nextNetwork(currentNetworks.toSet())

        if (nextNetwork == null) {
            throw RuntimeException("could not determine next network CIDR")
        }

        val networkResourceGroup = provisioner.createResourceGroup("network")

        val network = Network(networkName(tenant), nextNetwork, defaultLabels(tenant))
        networkResourceGroup.addResource(network)

        val subnet = Subnet(subnetForNetwork(nextNetwork), network)
        networkResourceGroup.addResource(subnet)
    }
}
