package de.solidblocks.cli.cloud.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.config.SpringContextUtil
import mu.KotlinLogging

class TenantBootstrapCommand : CliktCommand(name = "tenant-bootstrap", help = "bootstrap a cloud") {

    private val logger = KotlinLogging.logger {}

    val name: String by option(help = "cloud name").required()

    val environment: String by option(help = "cloud environment").required()

    override fun run() {
        logger.info { "creating/updating environment '${environment}' for cloud '$name'" }

        SpringContextUtil.callBeanAndShutdown(TenantMananger::class.java) {
            it.bootstrap(name, environment)
        }
    }
}
