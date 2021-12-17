package de.solidblocks.cli.commands.tenants

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.commands.BaseCloudEnvironmentCommand
import de.solidblocks.cloud.SolidblocksAppplicationContext

class TenantCreateCommand :
    BaseCloudEnvironmentCommand(
        name = "create",
        help = "create a new tenant"
    ) {

    val tenant: String by option(help = "tenant name").required()

    override fun run() {
        val context = SolidblocksAppplicationContext(solidblocksDatabaseUrl)
        context.configurationManager.createTenant(tenant, cloud, environment)
    }
}
