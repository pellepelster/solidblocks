package de.solidblocks.cli.commands.environment

import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import de.solidblocks.cli.commands.CommandApplicationContext
import mu.KotlinLogging
import kotlin.system.exitProcess

class EnvironmentDestroyCommand :
    BaseCloudEnvironmentCommand(name = "destroy", help = "destroy environment") {

    private val logger = KotlinLogging.logger {}

    val destroyVolumes by option("--destroy-volumes").flag(default = false)

    override fun run() {
        val context = CommandApplicationContext(solidblocksDatabaseUrl)

        if (!context.managers.environments.verifyReference(environmentRef)) {
            exitProcess(1)
        }

        if (!context.provisionerContext.createEnvironmentProvisioner(environmentRef).destroy(destroyVolumes)) {
            logger.error { "destroying environment failed" }
            exitProcess(1)
        }
    }
}
