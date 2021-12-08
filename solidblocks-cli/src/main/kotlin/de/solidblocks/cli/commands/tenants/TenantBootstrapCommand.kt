package de.solidblocks.cli.commands.tenants

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.base.ProvisionerRegistry
import de.solidblocks.cli.commands.BaseCloudEnvironmentCommand
import de.solidblocks.cloud.TenantMananger
import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.hetzner.Hetzner
import mu.KotlinLogging

class TenantBootstrapCommand : BaseCloudEnvironmentCommand(name = "bootstrap", help = "bootstrap a tenant") {

    private val logger = KotlinLogging.logger {}

    val tenant: String by option(help = "tenant name").required()

    override fun run() {

        val configurationManager = CloudConfigurationManager(solidblocksDatabase().dsl)
        val provisionerRegistry = ProvisionerRegistry()
        val provisioner = Provisioner(provisionerRegistry)

        val environmentConfiguration = configurationManager.environmentByName(cloud, environment)

        val tenantManager = TenantMananger(
            provisioner,
            configurationManager,
            Hetzner.createCloudApi(environmentConfiguration)
        )

        tenantManager.bootstrap(cloud, environment, tenant)
    }
}
