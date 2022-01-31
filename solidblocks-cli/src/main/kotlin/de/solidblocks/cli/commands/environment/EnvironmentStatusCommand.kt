package de.solidblocks.cli.commands.environment

import de.solidblocks.cli.commands.CommandApplicationContext
import kotlin.system.exitProcess

class EnvironmentStatusCommand :
    BaseCloudEnvironmentCommand(name = "status", help = "retrieve status for a cloud environment") {

    override fun run() {
        val context = CommandApplicationContext(solidblocksDatabaseUrl)
        if (!context.managers.environments.verifyReference(environmentRef)) {
            exitProcess(1)
        }

        // context.provisionerContext.createEnvironmentProvisioner(environmentRef).hasChanges()
    }
}
