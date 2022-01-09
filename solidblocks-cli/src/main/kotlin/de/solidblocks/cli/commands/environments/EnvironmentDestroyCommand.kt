package de.solidblocks.cli.commands.environments

import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import de.solidblocks.cli.commands.BaseCloudEnvironmentCommand
import de.solidblocks.cloud.SolidblocksAppplicationContext
import mu.KotlinLogging
import kotlin.system.exitProcess

class EnvironmentDestroyCommand :
    BaseCloudEnvironmentCommand(name = "destroy", help = "destroy environment") {

    private val logger = KotlinLogging.logger {}

    val destroyVolumes by option("--destroy-volumes").flag(default = false)

    override fun run() {
        val context = SolidblocksAppplicationContext(solidblocksDatabaseUrl)

        if (!context.verifyReference(reference)) {
            exitProcess(1)
        }

        if (!context.createEnvironmentProvisioner(reference).destroy(destroyVolumes)) {
            logger.error { "destroying environment failed" }
            exitProcess(1)
        }
    }
}
