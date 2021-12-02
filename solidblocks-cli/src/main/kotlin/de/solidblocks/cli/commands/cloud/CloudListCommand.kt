package de.solidblocks.cli.commands.cloud

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import de.solidblocks.cli.cloud.commands.BaseCloudSpringCommand
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
