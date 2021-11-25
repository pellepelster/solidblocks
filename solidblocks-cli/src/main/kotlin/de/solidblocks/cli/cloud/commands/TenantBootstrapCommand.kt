package de.solidblocks.cli.cloud.commands

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.self.BaseSpringCommand
import mu.KotlinLogging
import org.springframework.context.ApplicationContext

class TenantBootstrapCommand : BaseSpringCommand(name = "tenant-bootstrap", help = "bootstrap a cloud") {

    private val logger = KotlinLogging.logger {}

    val cloud: String by option(help = "cloud name").required()

    val environment: String by option(help = "cloud environment").required()

    override fun run(applicationContext: ApplicationContext) {
        TODO("Not yet implemented")
    }
}
