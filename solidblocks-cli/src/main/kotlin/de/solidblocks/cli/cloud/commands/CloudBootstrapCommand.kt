package de.solidblocks.cli.cloud.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.config.GlobalConfig
import de.solidblocks.cli.config.SpringContextUtil
import mu.KotlinLogging

class CloudBootstrapCommand : CliktCommand(name = "bootstrap", help = "bootstrap a cloud") {

    private val logger = KotlinLogging.logger {}

    val name: String by option(help = "cloud name").required()

    override fun run() {
        val environment = currentContext.findObject<GlobalConfig>()!!.environment
        logger.info { "creating/updating cloud '$name' for environment '${environment}'" }

        SpringContextUtil.callBeanAndShutdown(CloudMananger::class.java) {
            it.bootstrap(name, environment)
        }
    }
}
