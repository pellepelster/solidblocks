package de.solidblocks.cli.commands.environment

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import de.solidblocks.cli.commands.CommandApplicationContext
import kotlin.system.exitProcess

class EnvironmentConfigCommand :
    BaseCloudEnvironmentCommand(name = "config", help = "list all cloud configurations") {

    override fun run() {
        val context = CommandApplicationContext(solidblocksDatabaseUrl)

        if (!context.managers.environments.verifyReference(environmentRef)) {
            exitProcess(1)
        }

        println(
            ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(context.repositories.environments.getEnvironment(environmentRef))
        )
    }
}
