package de.solidblocks.cli.cloud.commands

import de.solidblocks.cli.self.BaseSpringCommand
import mu.KotlinLogging

class TenantBootstrapCommand : BaseSpringCommand(name = "tenant-bootstrap", help = "bootstrap a cloud") {

    private val logger = KotlinLogging.logger {}

    override fun run() {
        TODO("Not yet implemented")
    }
}
