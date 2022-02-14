package de.solidblocks.cloud.tenants

import de.solidblocks.base.reference.TenantReference
import de.solidblocks.cloud.VaultCloudConfiguration.createTenantVaultConfig
import de.solidblocks.cloud.model.ModelConstants
import de.solidblocks.cloud.model.ModelConstants.defaultTenantLabels
import de.solidblocks.cloud.model.ModelConstants.networkName
import de.solidblocks.cloud.model.entities.TenantEntity
import de.solidblocks.cloud.model.repositories.EnvironmentsRepository
import de.solidblocks.cloud.model.repositories.TenantsRepository
import de.solidblocks.cloud.status.Status
import de.solidblocks.cloud.utils.NetworkUtils.subnetForNetwork
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.hetzner.cloud.network.Network
import de.solidblocks.provisioner.hetzner.cloud.network.Subnet
import de.solidblocks.provisioner.hetzner.cloud.ssh.SshKey
import mu.KotlinLogging

class TenantProvisioner(
    reference: TenantReference,
    val provisioner: Provisioner,
    val environmentsRepository: EnvironmentsRepository,
    val statusManager: TenantsStatusManager,
    val repository: TenantsRepository
) {
    private val logger = KotlinLogging.logger {}

    val tenant: TenantEntity

    init {
        tenant = repository.getTenant(reference) ?: throw RuntimeException("tenant '$reference' not found")
        val environment = environmentsRepository.getEnvironment(reference) ?: throw RuntimeException("environment '$reference' not found")

        createTenantModel(
            tenant,
            setOf(SshKey(environment.name, environment.sshSecrets.sshPublicKey))
        )
    }

    fun destroy(): Boolean {
        return provisioner.destroy(false)
    }

    fun apply(): Boolean {
        statusManager.updateStatus(tenant, Status.PROVISIONING)
        logger.info { "applying tenant '${tenant.reference}'" }

        return provisioner.apply().also {
            statusManager.updateStatus(tenant, if (it) Status.OK else Status.ERROR)
        }
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
