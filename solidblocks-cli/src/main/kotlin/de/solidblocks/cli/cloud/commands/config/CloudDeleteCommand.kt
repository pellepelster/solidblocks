package de.solidblocks.cli.cloud.commands.config

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.config.SpringContextUtil
import de.solidblocks.cloud.config.CloudConfigurationManager
import mu.KotlinLogging
import org.springframework.stereotype.Component
import kotlin.io.path.ExperimentalPathApi

@Component
class CloudDeleteCommand :
    CliktCommand(name = "delete", help = "delete a cloud configuration") {

    val name: String by option(help = "name of the cloud").required()

    private val logger = KotlinLogging.logger {}

    @OptIn(ExperimentalPathApi::class)
    override fun run() {
        SpringContextUtil.bean(CloudConfigurationManager::class.java).delete(name)
    }
}
