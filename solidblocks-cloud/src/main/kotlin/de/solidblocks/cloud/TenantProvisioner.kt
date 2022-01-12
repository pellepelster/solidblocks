package de.solidblocks.cloud

import de.solidblocks.base.TenantReference
import de.solidblocks.cloud.NetworkUtils.subnetForNetwork
import de.solidblocks.cloud.VaultCloudConfiguration.createTenantVaultConfig
import de.solidblocks.cloud.model.EnvironmentRepository
import de.solidblocks.cloud.model.ModelConstants
import de.solidblocks.cloud.model.ModelConstants.defaultTenantLabels
import de.solidblocks.cloud.model.ModelConstants.networkName
import de.solidblocks.cloud.model.TenantRepository
import de.solidblocks.cloud.model.entities.TenantEntity
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.hetzner.cloud.network.Network
import de.solidblocks.provisioner.hetzner.cloud.network.Subnet
import de.solidblocks.provisioner.hetzner.cloud.ssh.SshKey
import mu.KotlinLogging

class TenantProvisioner(
    reference: TenantReference,
    val provisioner: Provisioner,
    val environmentRepository: EnvironmentRepository,
    tenantRepository: TenantRepository
) {
    private val logger = KotlinLogging.logger {}

    val tenant: TenantEntity

    init {
        tenant = tenantRepository.getTenant(reference)
        val environment = environmentRepository.getEnvironment(reference)

        createTenantModel(
            tenant,
            setOf(SshKey(environment.name, environment.sshSecrets.sshPublicKey))
        )
    }

    fun destroy(): Boolean {
        return provisioner.destroy(false)
    }

    fun bootstrap(): Boolean {
        return provisioner.apply()
    }

    private fun createTenantModel(
        tenant: TenantEntity,
        sshKeys: Set<SshKey> = emptySet()
    ) {
        val networkResourceGroup = provisioner.createResourceGroup("network")

        val networkCidr = tenant.getConfigValue(ModelConstants.TENANT_NETWORK_CIDR_KEY)

        val network = Network(networkName(tenant.reference), networkCidr, defaultTenantLabels(tenant.reference))
        networkResourceGroup.addResource(network)

        val subnet = Subnet(subnetForNetwork(networkCidr), network)
        networkResourceGroup.addResource(subnet)

        provisioner.addResourceGroup(createTenantVaultConfig(setOf(networkResourceGroup), tenant))
    }
}
