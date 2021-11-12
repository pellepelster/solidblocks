package de.solidblocks.cli.cloud.commands

import de.solidblocks.api.resources.dns.DnsRecord
import de.solidblocks.api.resources.dns.DnsZone
import de.solidblocks.api.resources.infrastructure.compute.Server
import de.solidblocks.api.resources.infrastructure.compute.UserDataDataSource
import de.solidblocks.api.resources.infrastructure.compute.Volume
import de.solidblocks.api.resources.infrastructure.network.FloatingIp
import de.solidblocks.api.resources.infrastructure.network.Network
import de.solidblocks.api.resources.infrastructure.ssh.SshKey
import de.solidblocks.api.resources.infrastructure.utils.ConstantDataSource
import de.solidblocks.cloud.config.CloudConfigurationManager
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

        createCloudModel(environment, rootDomain)

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

        createCloudModel(environmentName, cloudConfig.rootDomain, setOf(SshKey(environment.name, environment.sshConfig.sshPublicKey)))

        return provisioner.apply()
    }

    private fun createCloudModel(
            environment: String,
            rootDomain: String,
            sshKeys: Set<SshKey> = emptySet()
    ) {
        val rootZone = DnsZone(rootDomain)

        val id = "solidblocks"
        //val rootZone = DnsZone(cloud.solidblocksConfig.domain)

        val subnet = SubnetUtils("10.0.0.0/32").info
        val network = Network(id, subnet)

        val networkLayer = provisioner.createLayer("networkLayer")
        networkLayer.addResource(network)

        val location = "nbg1"

        val vaultLayer = provisioner.createLayer("vaultLayer")

        val vault1Volume = Volume("vault-1", location)
        val vault1FloatingIp = FloatingIp("vault-1", location, mapOf("role" to "vault", "name" to "vault-1"))
        val vault1Record = DnsRecord("vault-1.${environment}", vault1FloatingIp, zone = rootZone)

        val variables = HashMap<String, IDataSource<String>>()
        variables["cloud_name"] = ConstantDataSource("XXX")

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

        vaultLayer.addResource(vault1Record)
        vaultLayer.addResource(vault1Volume)
    }

}
