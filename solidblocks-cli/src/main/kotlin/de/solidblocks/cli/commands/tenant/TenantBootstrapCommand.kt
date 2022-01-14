package de.solidblocks.cli.commands.tenant

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.base.TenantReference
import de.solidblocks.cli.commands.environment.BaseCloudEnvironmentCommand
import de.solidblocks.cloud.ApplicationContext
import mu.KotlinLogging
import kotlin.system.exitProcess

class TenantBootstrapCommand : BaseCloudEnvironmentCommand(name = "bootstrap", help = "bootstrap a tenant") {

    private val logger = KotlinLogging.logger {}

    val tenant: String by option(help = "tenant name").required()

    override fun run() {

        val context = ApplicationContext(solidblocksDatabaseUrl)

        val tenant = TenantReference(cloud, environment, tenant)
        if (!context.verifyTenantReference(tenant)) {
            exitProcess(1)
        }

        context.createTenantProvisioner(tenant).bootstrap()
    }
}
