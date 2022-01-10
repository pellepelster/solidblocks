package de.solidblocks.cli.commands.environment

import de.solidblocks.cloud.SolidblocksAppplicationContext
import kotlin.system.exitProcess

class EnvironmentBootstrapCommand :
    BaseCloudEnvironmentCommand(name = "bootstrap", help = "bootstrap a cloud environment") {

    override fun run() {
        val context = SolidblocksAppplicationContext(solidblocksDatabaseUrl)
        if (!context.verifyReference(reference)) {
            exitProcess(1)
        }

        context.createEnvironmentProvisioner(reference).bootstrap()
    }
}
