package de.solidblocks.cli.cloud.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.config.SpringContextUtil
import mu.KotlinLogging

class CloudDestroyCommand :
    CliktCommand(name = "destroy", help = "bootstrap a cloud") {

    private val logger = KotlinLogging.logger {}

    val name: String by option(help = "cloud name").required()

    val environment: String by option(help = "cloud environment").required()

    override fun run() {
        logger.info { "destroying environment '${environment}' for cloud '$name'" }
        SpringContextUtil.callBeanAndShutdown(CloudMananger::class.java) {
            it.destroy(name, environment)
        }
    }
}
