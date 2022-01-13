package de.solidblocks.cloud

import de.solidblocks.api.resources.ResourceGroup
import de.solidblocks.base.ServiceReference
import de.solidblocks.base.lookups.ConstantDataSource
import de.solidblocks.cloud.model.EnvironmentRepository
import de.solidblocks.cloud.model.ModelConstants
import de.solidblocks.cloud.model.ModelConstants.defaultServiceLabels
import de.solidblocks.cloud.model.ModelConstants.networkName
import de.solidblocks.cloud.model.ModelConstants.serverName
import de.solidblocks.cloud.model.ModelConstants.sshKeyName
import de.solidblocks.cloud.model.ModelConstants.volumeName
import de.solidblocks.cloud.model.entities.Role
import de.solidblocks.core.IResourceLookup
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.hetzner.cloud.network.NetworkLookup
import de.solidblocks.provisioner.hetzner.cloud.server.Server
import de.solidblocks.provisioner.hetzner.cloud.server.UserData
import de.solidblocks.provisioner.hetzner.cloud.ssh.SshKeyLookup
import de.solidblocks.provisioner.hetzner.cloud.volume.Volume
import de.solidblocks.provisioner.hetzner.dns.zone.DnsZone
import de.solidblocks.provisioner.vault.policy.VaultPolicy
import de.solidblocks.vault.EnvironmentVaultManager
import de.solidblocks.vault.VaultConstants.issueTenantServerCertificatesPolicy
import de.solidblocks.vault.VaultConstants.providersGithubPolicy
import de.solidblocks.vault.VaultConstants.readEnvironmentClientCaCertificatePolicy
import de.solidblocks.vault.VaultConstants.servicePolicyName
import de.solidblocks.vault.VaultConstants.tokenSelfLookupPolicy
import de.solidblocks.vault.VaultConstants.tokenSelfRenewalPolicy
import mu.KotlinLogging

class ServiceProvisioner(
    val provisioner: Provisioner,
    val reference: ServiceReference,
    val environmentRepository: EnvironmentRepository,
    val vaultManager: EnvironmentVaultManager
) {

    companion object {
        fun createVaultConfigResourceGroup(reference: ServiceReference): ResourceGroup {
            val vaultConfigResourceGroup = ResourceGroup("vault_config")

            val servicePolicy = VaultPolicy(
                servicePolicyName(reference),
                setOf(
                    providersGithubPolicy(reference),
                    issueTenantServerCertificatesPolicy(reference),
                    readEnvironmentClientCaCertificatePolicy(reference),
                    tokenSelfRenewalPolicy(),
                    tokenSelfLookupPolicy()
                )
            )
            vaultConfigResourceGroup.addResource(servicePolicy)

            return vaultConfigResourceGroup
        }
    }

    private val logger = KotlinLogging.logger {}

    init {
        createServiceModel()
    }

    fun createServiceNode(name: String, location: String, resourceGroup: ResourceGroup, index: Int = 0) {
        val volume = resourceGroup.addResource(
            Volume(
                name = volumeName(reference, location, index),
                location = location,
                labels = ModelConstants.defaultCloudLabels(reference, Role.service)
            )
        )

        val environment = environmentRepository.getEnvironment(reference)
        val rootZone = DnsZone(environment.cloud.rootDomain)

        val staticVariables = HashMap<String, IResourceLookup<String>>()
        val ephemeralVariables = HashMap<String, IResourceLookup<String>>()

        val network = NetworkLookup(networkName(reference))
        val sshKey = SshKeyLookup(sshKeyName(reference))

        staticVariables.putAll(
            defaultCloudInitVariables(
                name, environment, rootZone, volume, vaultManager.createServiceToken(name, reference)
            ) + mapOf(
                "solidblocks_service" to ConstantDataSource("solidblocks-helloworld-agent"),
                "solidblocks_tenant" to ConstantDataSource(reference.tenant)
            )
        )

        val userData =
            UserData("lib-cloud-init-generated/${Role.service}-cloud-init.sh", staticVariables, ephemeralVariables)

        resourceGroup.addResource(
            Server(
                name = serverName(reference, location, index),
                network = network,
                userData = userData,
                sshKeys = setOf(sshKey),
                location = location,
                volume = volume,
                labels = defaultServiceLabels(reference, Role.service),
                subnet = null
            )
        )
    }

    fun createServiceModel() {
        val vaultConfigResourceGroup = createVaultConfigResourceGroup(reference)
        provisioner.addResourceGroup(vaultConfigResourceGroup)

        val instancesResourceGroup = provisioner.createResourceGroup("services", setOf(vaultConfigResourceGroup))
        createServiceNode("test", "nbg1", instancesResourceGroup)
    }

    fun bootstrap(): Boolean {
        return provisioner.apply()
    }
}
