package de.solidblocks.cloud

import de.solidblocks.api.resources.infrastructure.network.Network
import de.solidblocks.api.resources.infrastructure.ssh.SshKey
import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.cloud.config.model.TenantConfiguration
import de.solidblocks.provisioner.Provisioner
import mu.KotlinLogging
import org.apache.commons.net.util.SubnetUtils
import org.springframework.stereotype.Component

@Component
class TenantMananger(
    val provisioner: Provisioner,
    val cloudConfigurationManager: CloudConfigurationManager
) {
    private val logger = KotlinLogging.logger {}

    fun destroy(): Boolean {
        return true
    }

    fun bootstrap(tenantName: String, cloudName: String, environmentName: String): Boolean {
        val environment = cloudConfigurationManager.getEnvironment(cloudName, environmentName) ?: return false
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
        val subnet = SubnetUtils("10.0.1.0/24").info
        val network = Network(tenant.name, SubnetUtils("10.0.1.0/24"))

        val networkResourceGroup = provisioner.createResourceGroup("network")
        networkResourceGroup.addResource(network)
    }
}
