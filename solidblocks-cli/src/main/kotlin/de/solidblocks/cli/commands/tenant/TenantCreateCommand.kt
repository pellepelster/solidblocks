package de.solidblocks.cli.commands.tenant

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.cli.commands.CommandApplicationContext
import de.solidblocks.cli.commands.environment.BaseCloudEnvironmentCommand
import de.solidblocks.cloud.CloudConstants.ADMIN_USER
import de.solidblocks.cloud.tenants.api.TenantCreateRequest
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
        val context = CommandApplicationContext(solidblocksDatabaseUrl)

        val reference = EnvironmentReference(cloud, environment)
        if (!context.managers.environments.verifyReference(reference)) {
            exitProcess(1)
        }

        context.managers.tenants.create(reference, ADMIN_USER, TenantCreateRequest(tenant, email, password))
    }
}
