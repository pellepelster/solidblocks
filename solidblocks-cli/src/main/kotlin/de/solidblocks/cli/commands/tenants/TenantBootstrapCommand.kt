package de.solidblocks.cli.commands.tenants

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.commands.BaseCloudEnvironmentCommand
import de.solidblocks.cloud.SolidblocksAppplicationContext
import mu.KotlinLogging

class TenantBootstrapCommand : BaseCloudEnvironmentCommand(name = "bootstrap", help = "bootstrap a tenant") {

    private val logger = KotlinLogging.logger {}

    val tenant: String by option(help = "tenant name").required()

    override fun run() {

        val context = SolidblocksAppplicationContext(solidblocksDatabaseUrl)

        /*
        val provisioner = Provisioner(provisionerRegistry)

        val environmentConfiguration = configurationManager.environmentByName(cloud, environment)

        val tenantManager = TenantManager(
            provisioner,
            configurationManager,
            Hetzner.createCloudApi(environmentConfiguration)
        )

        tenantManager.bootstrap(cloud, environment, tenant)
         */
    }
}
