package de.solidblocks.cli.cloud.commands

import de.solidblocks.api.resources.dns.DnsRecord
import de.solidblocks.api.resources.dns.DnsZone
import de.solidblocks.api.resources.dns.DnsZoneRuntime
import de.solidblocks.api.resources.infrastructure.compute.Server
import de.solidblocks.api.resources.infrastructure.compute.UserDataDataSource
import de.solidblocks.api.resources.infrastructure.compute.Volume
import de.solidblocks.api.resources.infrastructure.compute.VolumeRuntime
import de.solidblocks.api.resources.infrastructure.network.FloatingIp
import de.solidblocks.api.resources.infrastructure.network.FloatingIpAssignment
import de.solidblocks.api.resources.infrastructure.network.FloatingIpRuntime
import de.solidblocks.api.resources.infrastructure.network.Network
import de.solidblocks.api.resources.infrastructure.ssh.SshKey
import de.solidblocks.api.resources.infrastructure.utils.Base64Encode
import de.solidblocks.api.resources.infrastructure.utils.ConstantDataSource
import de.solidblocks.api.resources.infrastructure.utils.ResourceLookup
import de.solidblocks.cloud.config.CloudConfig
import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.cloud.config.CloudEnvironmentConfig
import de.solidblocks.core.IDataSource
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.hetzner.cloud.HetznerCloudCredentialsProvider
import de.solidblocks.provisioner.hetzner.cloud.getHetznerCloudApiToken
import de.solidblocks.provisioner.hetzner.dns.HetznerDnsCredentialsProvider
import de.solidblocks.provisioner.hetzner.dns.getHetznerDnsApiToken
import mu.KotlinLogging
import org.apache.commons.net.util.SubnetUtils
import org.springframework.stereotype.Component

@Component
class CloudMananger(
        val cloudCredentialsProvider: HetznerCloudCredentialsProvider,
        val dnsCredentialsProvider: HetznerDnsCredentialsProvider,
        val provisioner: Provisioner,
        val cloudConfigurationManager: CloudConfigurationManager
) {
    private val logger = KotlinLogging.logger {}

    fun destroy(environment: String, rootDomain: String, hetznerCloudApiToken: String, hetznerDnsApiToken: String) {
        cloudCredentialsProvider.addApiToken(hetznerCloudApiToken)
        dnsCredentialsProvider.addApiToken(hetznerDnsApiToken)

        //createCloudModel(environment, rootDomain)

        provisioner.destroyAll()
    }

    fun show(name: String): Boolean {
        return true
    }

    fun bootstrap(cloudName: String, environmentName: String): Boolean {
        val cloudConfig = cloudConfigurationManager.cloudByName(cloudName)
        if (cloudConfig == null) {
            logger.error { "cloud '${cloudName}' not found" }
            return false
        }

        val environment = cloudConfig.environments.filter { it.name == environmentName }.firstOrNull()
        if (environment == null) {
            logger.error { "cloud '${cloudName}' has no environment '${environment}'" }
            return false
        }


        cloudCredentialsProvider.addApiToken(environment.configValues.getHetznerCloudApiToken()!!.value)
        dnsCredentialsProvider.addApiToken(environment.configValues.getHetznerDnsApiToken()!!.value)

        createCloudModel(cloudConfig, environment, setOf(SshKey(environment.name, environment.sshConfig.sshPublicKey)))

        return provisioner.apply()
    }

    private fun createCloudModel(
            cloudConfig: CloudConfig,
            environment: CloudEnvironmentConfig,
            sshKeys: Set<SshKey> = emptySet()
    ) {
        val rootZone = DnsZone(cloudConfig.rootDomain)

        val id = "solidblocks"
        //val rootZone = DnsZone(cloud.solidblocksConfig.domain)

        val subnet = SubnetUtils("10.0.0.0/32").info
        val network = Network(id, subnet)

        val networkLayer = provisioner.createLayer("networkLayer")
        networkLayer.addResource(network)

        val location = "nbg1"

        val vaultLayer = provisioner.createLayer("vaultLayer")

        val vault1Volume = Volume("vault-1_${location}", location)
        val vault1FloatingIp = FloatingIp("vault-1", location, mapOf("role" to "vault", "name" to "vault-1"))
        val vault1Record = DnsRecord("vault-1.${environment.name}", vault1FloatingIp, zone = rootZone)

        val variables = HashMap<String, IDataSource<String>>()
        variables["cloud_name"] = ConstantDataSource(cloudConfig.name)
        variables["cloud_root_domain"] = ResourceLookup<DnsZoneRuntime>(rootZone) {
            it.name
        }
        variables["environment_name"] = ConstantDataSource(environment.name)
        variables["hostname"] = ConstantDataSource("vault-1")
        variables["public_ip"] = ResourceLookup<FloatingIpRuntime>(vault1FloatingIp) {
            it.ipv4
        }
        variables["ssh_identity_ed25519_key"] = Base64Encode(ConstantDataSource(environment.sshConfig.sshIdentityPrivateKey))
        variables["ssh_identity_ed25519_pub"] = Base64Encode(ConstantDataSource(environment.sshConfig.sshIdentityPublicKey))

        variables["storage_local_device"] = ResourceLookup<VolumeRuntime>(vault1Volume) {
            it.device
        }


        val userData = UserDataDataSource("lib-cloud-init/vault-cloud-init.sh", variables)
        val vault1Server = Server(
                "vault-1",
                network,
                userData,
                sshKeys = sshKeys,
                location = location,
                volume = vault1Volume,
                dependencies = listOf(vault1Record)
        )
        vaultLayer.addResource(vault1Server)
        vaultLayer.addResource(FloatingIpAssignment(server = vault1Server, floatingIp = vault1FloatingIp))

        vaultLayer.addResource(vault1Record)
        vaultLayer.addResource(vault1Volume)
    }

}
