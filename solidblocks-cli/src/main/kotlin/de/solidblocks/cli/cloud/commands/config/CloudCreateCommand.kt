package de.solidblocks.cli.cloud.commands.config

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.config.SpringContextUtil
import de.solidblocks.cloud.config.CloudConfigurationManager
import mu.KotlinLogging
import org.springframework.stereotype.Component
import kotlin.io.path.ExperimentalPathApi

@Component
class CloudCreateCommand :
    CliktCommand(name = "create", help = "create a new cloud configuration") {

    val name: String by option(help = "name of the cloud").required()

    val domain: String by option(help = "root domain for the cloud").required()

    private val logger = KotlinLogging.logger {}

    @OptIn(ExperimentalPathApi::class)
    override fun run() {

        logger.error { "creating cloud '$name'" }

        if (!SpringContextUtil.bean(CloudConfigurationManager::class.java).createCloud(
                        name,
                        domain)) {
            throw ProgramResult(1)
        }
    }
}
