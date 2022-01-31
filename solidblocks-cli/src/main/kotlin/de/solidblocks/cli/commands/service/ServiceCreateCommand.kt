package de.solidblocks.cli.commands.service

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.commands.CommandApplicationContext
import de.solidblocks.cli.commands.tenant.BaseCloudTenantCommand
import kotlin.system.exitProcess

class ServiceCreateCommand :
    BaseCloudTenantCommand(name = "create", help = "create new service") {

    val service: String by option(help = "service name").required()

    override fun run() {
        val context = CommandApplicationContext(solidblocksDatabaseUrl)

        if (!context.managers.tenants.verifyReference(tenantRef)) {
            exitProcess(1)
        }

        /*
        if (!context.serviceRepository.createService(tenantRef)) {
            exitProcess(1)
        }*/
    }
}
