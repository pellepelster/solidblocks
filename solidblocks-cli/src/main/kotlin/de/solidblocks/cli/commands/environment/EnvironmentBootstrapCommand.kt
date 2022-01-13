package de.solidblocks.cli.commands.environment

import de.solidblocks.cloud.AppplicationContext
import kotlin.system.exitProcess

class EnvironmentBootstrapCommand :
    BaseCloudEnvironmentCommand(name = "bootstrap", help = "bootstrap a cloud environment") {

    override fun run() {
        val context = AppplicationContext(solidblocksDatabaseUrl)
        if (!context.verifyEnvironmentReference(environmentRef)) {
            exitProcess(1)
        }

        context.createEnvironmentProvisioner(environmentRef).bootstrap()
    }
}
