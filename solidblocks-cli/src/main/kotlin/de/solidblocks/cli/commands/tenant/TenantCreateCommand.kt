package de.solidblocks.cli.commands.tenant

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.base.EnvironmentReference
import de.solidblocks.cli.commands.environment.BaseCloudEnvironmentCommand
import de.solidblocks.cloud.AppplicationContext
import kotlin.system.exitProcess

class TenantCreateCommand :
    BaseCloudEnvironmentCommand(
        name = "create",
        help = "create a new tenant"
    ) {

    val tenant: String by option(help = "tenant name").required()

    override fun run() {
        val context = AppplicationContext(solidblocksDatabaseUrl)

        val reference = EnvironmentReference(cloud, environment)
        if (!context.verifyEnvironmentReference(reference)) {
            exitProcess(1)
        }

        context.cloudManager.createTenant(reference.toTenant(tenant))
    }
}
