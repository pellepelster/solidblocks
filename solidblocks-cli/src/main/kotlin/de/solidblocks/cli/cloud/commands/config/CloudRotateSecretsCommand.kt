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
class CloudRotateSecretsCommand :
    CliktCommand(name = "rotate-secrets", help = "rotate cloud secrets") {

    val cloud: String by option(help = "name of the cloud").required()

    val environment: String by option(help = "cloud environment").required()

    @OptIn(ExperimentalPathApi::class)
    override fun run() {
        SpringContextUtil.callBeanAndShutdown(CloudConfigurationManager::class.java) {
            it.rotateEnvironmentSecrets(cloud, environment)
        }
    }
}
