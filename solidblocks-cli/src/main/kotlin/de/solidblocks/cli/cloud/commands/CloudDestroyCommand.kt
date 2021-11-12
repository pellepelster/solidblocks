package de.solidblocks.cli.cloud.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.config.SpringContextUtil
import mu.KotlinLogging
import kotlin.system.exitProcess

class CloudDestroyCommand :
    CliktCommand(name = "destroy", help = "bootstrap a cloud") {

    private val logger = KotlinLogging.logger {}

    val name: String by option(help = "cloud name").required()

    override fun run() {
        logger.info { "destroying cloud '$name'" }
        SpringContextUtil.callBeanAndShutdown(CloudTenantMananger::class.java) {

            if (!it.cloudConfigurationManager.hasTenant(name)) {
                logger.error { "cloud '$name' not found" }
                exitProcess(1)
            }

            it.destroy(name)
        }
    }
}
