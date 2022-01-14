package de.solidblocks.cli.commands.tenant

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.base.EnvironmentReference
import de.solidblocks.cli.commands.environment.BaseCloudEnvironmentCommand
import de.solidblocks.cloud.ApplicationContext
import kotlin.system.exitProcess

class TenantCreateCommand :
    BaseCloudEnvironmentCommand(
        name = "create",
        help = "create a new tenant"
    ) {

    val tenant: String by option(help = "tenant name").required()

    val email: String by option(help = "tenant admin email").required()

    val password: String by option(help = "tenant admin password").required()

    override fun run() {
        val context = ApplicationContext(solidblocksDatabaseUrl)

        val reference = EnvironmentReference(cloud, environment)
        if (!context.verifyEnvironmentReference(reference)) {
            exitProcess(1)
        }

        context.tenantsManager.create(reference, tenant, email, password)
    }
}
