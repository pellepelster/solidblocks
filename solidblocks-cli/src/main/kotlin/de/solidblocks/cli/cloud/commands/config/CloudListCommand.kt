package de.solidblocks.cli.cloud.commands.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import de.solidblocks.cloud.config.CloudConfigurationManager
import org.springframework.stereotype.Component

@Component
class CloudListCommand :
    BaseCloudSpringCommand(name = "list", help = "list all cloud configurations") {

    override fun run() {
        runSpringApplication {
            it.getBean(CloudConfigurationManager::class.java).listClouds().let {
                println(ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(it))
            }
        }
    }

}

