package de.solidblocks.cli.cloud.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.config.SpringContextUtil
import mu.KotlinLogging
import kotlin.system.exitProcess

class CloudTestCommand :
    CliktCommand(name = "test", help = "command for testing purposes") {

    private val logger = KotlinLogging.logger {}

    val name: String by option(help = "cloud name").required()

    override fun run() {
        logger.info { "test cloud '$name'" }
        SpringContextUtil.callBeanAndShutdown(TenantMananger::class.java) {
            if (!it.cloudConfigurationManager.hasTenant(name)) {
                logger.error { "cloud '$name' not found" }
                exitProcess(1)
            }
        }
    }
}
