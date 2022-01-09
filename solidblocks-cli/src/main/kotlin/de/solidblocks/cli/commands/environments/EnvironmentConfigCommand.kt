package de.solidblocks.cli.commands.environments

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import de.solidblocks.cli.commands.BaseCloudEnvironmentCommand
import de.solidblocks.cloud.SolidblocksAppplicationContext
import kotlin.system.exitProcess

class EnvironmentConfigCommand :
    BaseCloudEnvironmentCommand(name = "config", help = "list all cloud configurations") {

    override fun run() {
        val context = SolidblocksAppplicationContext(solidblocksDatabaseUrl)

        if (!context.verifyReference(reference)) {
            exitProcess(1)
        }

        println(
            ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(context.environmentRepository.getEnvironment(reference))
        )
    }
}
