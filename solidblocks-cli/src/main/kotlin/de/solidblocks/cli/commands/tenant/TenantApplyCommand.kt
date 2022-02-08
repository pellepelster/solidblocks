package de.solidblocks.cli.commands.tenant

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.base.reference.TenantReference
import de.solidblocks.cli.commands.CommandApplicationContext
import de.solidblocks.cli.commands.environment.BaseCloudEnvironmentCommand
import mu.KotlinLogging
import kotlin.system.exitProcess

class TenantApplyCommand : BaseCloudEnvironmentCommand(name = "apply", help = "bootstrap a tenant") {

    private val logger = KotlinLogging.logger {}

    val tenant: String by option(help = "tenant name").required()

    override fun run() {

        val context = CommandApplicationContext(solidblocksDatabaseUrl)

        val tenant = TenantReference(cloud, environment, tenant)
        if (!context.managers.tenants.verifyReference(tenant)) {
            exitProcess(1)
        }

        context.provisionerContext.createTenantProvisioner(tenant).apply()
    }
}
