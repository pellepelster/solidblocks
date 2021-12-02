package de.solidblocks.cli.commands.tenants

import de.solidblocks.cli.self.BaseSpringCommand
import mu.KotlinLogging

class TenantBootstrapCommand : BaseSpringCommand(name = "bootstrap", help = "bootstrap a tenant") {

    private val logger = KotlinLogging.logger {}

    override fun run() {
        TODO("Not yet implemented")
    }
}
