package de.solidblocks.cli.cloud.commands.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.github.ajalt.clikt.core.CliktCommand
import de.solidblocks.cli.config.SpringContextUtil
import de.solidblocks.cloud.config.CloudConfigurationManager
import mu.KotlinLogging
import org.springframework.stereotype.Component
import kotlin.io.path.ExperimentalPathApi

@Component
class CloudListCommand :
    CliktCommand(name = "list", help = "list all cloud configurations") {

    private val logger = KotlinLogging.logger {}


    @OptIn(ExperimentalPathApi::class)
    override fun run() {

        SpringContextUtil.bean(CloudConfigurationManager::class.java).listClouds().let {
            println(ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(it))
        }
    }
}