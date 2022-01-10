package de.solidblocks.cli.commands.service

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.commands.tenant.BaseCloudTenantCommand
import de.solidblocks.cloud.SolidblocksAppplicationContext
import kotlin.system.exitProcess

class ServiceBootstrapCommand :
    BaseCloudTenantCommand(name = "bootstrap", help = "bootstrap new service") {

    val service: String by option(help = "service name").required()

    override fun run() {
        val context = SolidblocksAppplicationContext(solidblocksDatabaseUrl)

        if (!context.verifyTenantReference(tenantRef)) {
            exitProcess(1)
        }

        if (!context.createServiceProvisioner(tenantRef.toService(service)).bootstrap()) {
            exitProcess(1)
        }
    }
}
