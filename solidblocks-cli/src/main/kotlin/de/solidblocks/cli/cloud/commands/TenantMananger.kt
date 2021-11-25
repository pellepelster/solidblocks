package de.solidblocks.cli.cloud.commands

import de.solidblocks.api.resources.infrastructure.network.Network
import de.solidblocks.api.resources.infrastructure.ssh.SshKey
import de.solidblocks.cloud.config.model.CloudConfiguration
import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.cloud.config.model.CloudEnvironmentConfiguration
import de.solidblocks.cloud.config.model.TenantConfig
import de.solidblocks.provisioner.Provisioner
import mu.KotlinLogging
import org.apache.commons.net.util.SubnetUtils
import org.springframework.stereotype.Component
import java.util.*

@Component
class TenantMananger(
        val provisioner: Provisioner,
        val cloudConfigurationManager: CloudConfigurationManager
) {
    private val logger = KotlinLogging.logger {}

    fun destroy(cloudName: String, environmentName: String): Boolean {
        return true
    }

    fun bootstrap(cloudName: String, environmentName: String): Boolean {

        if (!cloudConfigurationManager.hasCloud(cloudName)) {
            logger.error { "cloud '${cloudName}' not found" }
            return false
        }

        val cloudConfig = cloudConfigurationManager.cloudByName(cloudName)

        val environment = cloudConfig.environments.filter { it.name == environmentName }.firstOrNull()
        if (environment == null) {
            logger.error { "cloud '${cloudName}' has no environment '${environment}'" }
            return false
        }


        createTenantModel(cloudConfig, environment, TenantConfig(UUID.randomUUID(), "tenant1"), setOf(SshKey(environment.name, environment.sshSecrets.sshPublicKey)))

        return provisioner.apply()
    }

    private fun createTenantModel(
        cloud: CloudConfiguration,
        environment: CloudEnvironmentConfiguration,
        tenant: TenantConfig,
        sshKeys: Set<SshKey> = emptySet()
    ) {
        val subnet = SubnetUtils("10.0.1.0/24").info
        val network = Network(tenant.name, SubnetUtils("10.0.1.0/24"))

        val networkResourceGroup = provisioner.createResourceGroup("network")
        networkResourceGroup.addResource(network)
    }

}
