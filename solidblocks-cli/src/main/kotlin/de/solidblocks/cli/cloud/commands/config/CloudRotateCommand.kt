package de.solidblocks.cli.cloud.commands.config

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.config.GlobalConfig
import de.solidblocks.cli.config.SpringContextUtil
import de.solidblocks.cloud.config.CloudConfigurationManager
import mu.KotlinLogging
import org.springframework.stereotype.Component
import kotlin.io.path.ExperimentalPathApi

@Component
class CloudRotateCommand :
    CliktCommand(name = "rotate", help = "rotate cloud secrets") {

    val name: String by option(help = "name of the cloud").required()

    private val logger = KotlinLogging.logger {}

    @OptIn(ExperimentalPathApi::class)
    override fun run() {
        val environment = currentContext.findObject<GlobalConfig>()!!.environment

        SpringContextUtil.callBeanAndShutdown(CloudConfigurationManager::class.java) {
            it.regenerateCloudSecrets(name, environment)
        }
    }
}
