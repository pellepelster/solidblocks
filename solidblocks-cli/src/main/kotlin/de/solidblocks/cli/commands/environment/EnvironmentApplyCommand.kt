package de.solidblocks.cli.commands.environment

import de.solidblocks.cli.commands.CommandApplicationContext
import kotlin.system.exitProcess

class EnvironmentApplyCommand :
    BaseCloudEnvironmentCommand(name = "apply", help = "bootstrap a cloud environment") {

    override fun run() {
        val context = CommandApplicationContext(solidblocksDatabaseUrl)
        if (!context.managers.environments.verifyReference(environmentRef)) {
            exitProcess(1)
        }

        context.provisionerContext.createEnvironmentProvisioner(environmentRef).apply()
    }
}
