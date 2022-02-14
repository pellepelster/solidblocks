package de.solidblocks.cloud.environments

import de.solidblocks.api.resources.ResourceGroup
import de.solidblocks.base.BaseConstants.environmentId
import de.solidblocks.base.BaseConstants.serverName
import de.solidblocks.base.BaseConstants.vaultTokenName
import de.solidblocks.base.lookups.Base64Encode
import de.solidblocks.base.lookups.ConstantDataSource
import de.solidblocks.base.lookups.CustomDataSource
import de.solidblocks.base.lookups.ResourceLookup
import de.solidblocks.base.solidblocksVersion
import de.solidblocks.cloud.VaultCloudConfiguration
import de.solidblocks.cloud.model.ModelConstants.backupUiAddress
import de.solidblocks.cloud.model.ModelConstants.defaultEnvironmentLabels
import de.solidblocks.cloud.model.ModelConstants.floatingIpName
import de.solidblocks.cloud.model.ModelConstants.networkName
import de.solidblocks.cloud.model.ModelConstants.vaultAddress
import de.solidblocks.cloud.model.ModelConstants.volumeName
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.cloud.model.entities.NodeRole
import de.solidblocks.cloud.status.Status
import de.solidblocks.cloud.status.StatusManager
import de.solidblocks.cloud.utils.HttpHealthCheck
import de.solidblocks.cloud.utils.NetworkUtils.solidblocksNetwork
import de.solidblocks.cloud.utils.NetworkUtils.subnetForNetwork
import de.solidblocks.core.IResourceLookup
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.hetzner.cloud.floatingip.FloatingIp
import de.solidblocks.provisioner.hetzner.cloud.floatingip.FloatingIpAssignment
import de.solidblocks.provisioner.hetzner.cloud.floatingip.FloatingIpRuntime
import de.solidblocks.provisioner.hetzner.cloud.network.Network
import de.solidblocks.provisioner.hetzner.cloud.network.Subnet
import de.solidblocks.provisioner.hetzner.cloud.server.Server
import de.solidblocks.provisioner.hetzner.cloud.server.UserData
import de.solidblocks.provisioner.hetzner.cloud.ssh.SshKey
import de.solidblocks.provisioner.hetzner.cloud.volume.Volume
import de.solidblocks.provisioner.hetzner.cloud.volume.VolumeRuntime
import de.solidblocks.provisioner.hetzner.dns.record.DnsRecord
import de.solidblocks.provisioner.hetzner.dns.zone.DnsZone
import de.solidblocks.provisioner.hetzner.dns.zone.DnsZoneRuntime
import de.solidblocks.provisioner.vault.VaultHealthCheck
import de.solidblocks.provisioner.vault.VaultRootClientProvider
import de.solidblocks.vault.EnvironmentVaultManager
import de.solidblocks.vault.VaultConstants.backupPolicyName
import mu.KotlinLogging

public fun defaultCloudInitVariables(name: String, environment: EnvironmentEntity, rootZone: DnsZone, volume: Volume, vaultToken: String): Map<out String, IResourceLookup<String>> {
    return mapOf(
        "solidblocks_cloud" to ConstantDataSource(environment.cloud.name), "solidblocks_hostname" to ConstantDataSource(name), "solidblocks_version" to ConstantDataSource(solidblocksVersion()), "vault_token" to ConstantDataSource(vaultToken),
        "solidblocks_root_domain" to ResourceLookup<DnsZoneRuntime>(rootZone) {
            it.name
        },
        "vault_addr" to ConstantDataSource(vaultAddress(environment)), "solidblocks_environment" to ConstantDataSource(environment.name), "ssh_identity_ed25519_key" to Base64Encode(ConstantDataSource(environment.sshSecrets.sshIdentityPrivateKey)), "ssh_identity_ed25519_pub" to Base64Encode(ConstantDataSource(environment.sshSecrets.sshIdentityPublicKey)),
        "storage_local_device" to ResourceLookup<VolumeRuntime>(volume) {
            it.device
        }

    )
}

public fun defaultCloudInitVariables(name: String, environment: EnvironmentEntity, rootZone: DnsZone, volume: Volume, vaultToken: String, floatingIp: FloatingIp): Map<out String, IResourceLookup<String>> {
    return mapOf(
        "solidblocks_public_ip" to ResourceLookup<FloatingIpRuntime>(floatingIp) {
            it.ipv4
        },
    ) + defaultCloudInitVariables(name, environment, rootZone, volume, vaultToken)
}

class EnvironmentProvisioner(private val environment: EnvironmentEntity, private val vaultRootClientProvider: VaultRootClientProvider, private val provisioner: Provisioner, val statusManager: StatusManager) {
    private val logger = KotlinLogging.logger {}

    init {
        createEnvironmentModel(environment, setOf(SshKey(environmentId(environment.reference), environment.sshSecrets.sshPublicKey)))
    }

    fun destroy(destroyVolumes: Boolean): Boolean {
        return provisioner.destroyAll(destroyVolumes)
    }

    fun apply(): Boolean {
        statusManager.updateStatus(environment.id, Status.PROVISIONING)
        logger.info { "applying environment '${environment.reference}'" }

        return provisioner.apply().also {
            statusManager.updateStatus(environment.id, if (it) Status.OK else Status.ERROR)
        }
    }

    private fun createEnvironmentModel(environment: EnvironmentEntity, sshKeys: Set<SshKey>) {
        val rootZone = DnsZone(environment.cloud.rootDomain)

        val networkResourceGroup = provisioner.createResourceGroup("network")

        val network = Network(networkName(environment.reference), solidblocksNetwork(), defaultEnvironmentLabels(environment.reference))
        networkResourceGroup.addResource(network)

        val subnet = Subnet(subnetForNetwork(solidblocksNetwork()), network)
        networkResourceGroup.addResource(subnet)

        val location = "nbg1"

        val vaultResourceGroup = provisioner.createResourceGroup("vault")
        createVaultServers(location, rootZone, vaultResourceGroup, network, subnet, sshKeys)

        provisioner.addResourceGroup(VaultCloudConfiguration.createEnvironmentVaultConfig(setOf(vaultResourceGroup), environment))

        val backupResourceGroup = provisioner.createResourceGroup("backup", setOf(vaultResourceGroup))
        createBackupServers(location, rootZone, backupResourceGroup, network, subnet, sshKeys)
    }

    private fun createBackupServers(location: String, rootZone: DnsZone, resourceGroup: ResourceGroup, network: Network, subnet: Subnet, sshKeys: Set<SshKey>) {
        val index = 0
        val name = "backup"
        val nodeRole = NodeRole.backup

        val volume = createVolume(name, location, index, nodeRole)
        resourceGroup.addResource(volume)

        val floatingIp = createFloatingIp(name, location, index, nodeRole)
        resourceGroup.addResource(floatingIp)

        val staticVariables = HashMap<String, IResourceLookup<String>>()
        val ephemeralVariables = HashMap<String, IResourceLookup<String>>()

        staticVariables.putAll(defaultCloudInitVariables(name, environment, rootZone, volume, "<none>", floatingIp))

        ephemeralVariables["vault_token"] = CustomDataSource {
            val vaultManager = EnvironmentVaultManager(vaultRootClientProvider.createClient(), environment.reference)
            vaultManager.createEnvironmentToken(vaultTokenName(name, environment.reference, location, index), backupPolicyName(environment.reference))
        }

        val userData = UserData("lib-cloud-init-generated/$nodeRole-cloud-init.sh", staticVariables, ephemeralVariables)
        val server = Server(name = serverName(name, environment.reference, location, index), network = network, subnet = subnet, userData = userData, sshKeys = sshKeys, location = location, volume = volume, labels = defaultEnvironmentLabels(environment.reference, nodeRole), dependencies = setOf(floatingIp))
        resourceGroup.addResource(server)

        resourceGroup.addResource(DnsRecord(name = "$name-$index.${environment.name}", floatingIp = floatingIp, dnsZone = rootZone, server = server))

        val floatingIpAssignment = FloatingIpAssignment(server = server, floatingIp = floatingIp)
        resourceGroup.addResource(floatingIpAssignment)

        resourceGroup.addResource(DnsRecord(name = "$name.${environment.name}", floatingIp = floatingIp, dnsZone = rootZone, server = server))
    }

    private fun createVaultServers(location: String, rootZone: DnsZone, resourceGroup: ResourceGroup, network: Network, subnet: Subnet, sshKeys: Set<SshKey>) {
        val index = 0
        val name = "vault"
        val nodeRole = NodeRole.vault

        val volume = createVolume(name, location, index, nodeRole)
        resourceGroup.addResource(volume)

        val floatingIp = createFloatingIp(name, location, index, nodeRole)
        resourceGroup.addResource(floatingIp)

        val staticVariables = HashMap<String, IResourceLookup<String>>()
        val ephemeralVariables = HashMap<String, IResourceLookup<String>>()

        staticVariables.putAll(defaultCloudInitVariables(name, environment, rootZone, volume, "<none>", floatingIp))

        val userData = UserData("lib-cloud-init-generated/$nodeRole-cloud-init.sh", staticVariables, ephemeralVariables)
        val server = Server(name = serverName(name, environment.reference, location, index), network = network, subnet = subnet, userData = userData, sshKeys = sshKeys, location = location, volume = volume, labels = defaultEnvironmentLabels(environment.reference, nodeRole), dependencies = setOf(floatingIp))
        resourceGroup.addResource(server)

        resourceGroup.addResource(DnsRecord(name = "$name-$index.${environment.name}", floatingIp = floatingIp, dnsZone = rootZone, server = server))

        val floatingIpAssignment = FloatingIpAssignment(server = server, floatingIp = floatingIp)
        resourceGroup.addResource(floatingIpAssignment)

        resourceGroup.addResource(object : DnsRecord(name = "$name.${environment.name}", floatingIp = floatingIp, floatingIpAssignment = floatingIpAssignment, dnsZone = rootZone, server = server) {
            override val healthCheck = health@{
                HttpHealthCheck.check("https://${this.name}.${dnsZone.name}:8200")
            }
        })
    }

    private fun createVolume(name: String, location: String, index: Int, nodeRole: NodeRole) = Volume(name = volumeName(name, environment.reference, location, index), location = location, labels = defaultEnvironmentLabels(environment.reference, nodeRole))

    private fun createFloatingIp(name: String, location: String, index: Int, nodeRole: NodeRole) = FloatingIp(name = floatingIpName(name, environment.reference, location, index), location = location, labels = defaultEnvironmentLabels(environment.reference, nodeRole))

    fun healthcheck(): Boolean {
        logger.info { "running healthcheck for environment '${environment.reference}'" }
        val result =
            HttpHealthCheck.check(vaultAddress(environment)) && HttpHealthCheck.check(backupUiAddress(environment)) && VaultHealthCheck.check(
                vaultAddress(environment)
            )

        statusManager.updateStatus(environment.id, if (result) Status.OK else Status.ERROR)

        return result
    }
}
