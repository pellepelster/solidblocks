package de.solidblocks.cli.commands.environments

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import de.solidblocks.cli.commands.BaseCloudEnvironmentCommand
import de.solidblocks.cloud.config.CloudConfigurationManager

class EnvironmentConfigCommand :
    BaseCloudEnvironmentCommand(name = "config", help = "list all cloud configurations") {

    override fun run() {
        val cloudConfigurationManager = CloudConfigurationManager(solidblocksDatabase().dsl)
        println(ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(cloudConfigurationManager.environmentByName(cloud, environment)))
    }
}
