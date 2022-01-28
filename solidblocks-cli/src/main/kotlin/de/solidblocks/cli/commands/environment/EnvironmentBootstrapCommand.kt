package de.solidblocks.cli.commands.environment

import de.solidblocks.cloud.ApplicationContext
import kotlin.system.exitProcess

class EnvironmentBootstrapCommand :
    BaseCloudEnvironmentCommand(name = "bootstrap", help = "bootstrap a cloud environment") {

    override fun run() {
        val context = ApplicationContext(solidblocksDatabaseUrl)
        if (!context.managers.environments.verifyReference(environmentRef)) {
            exitProcess(1)
        }

        context.createEnvironmentProvisioner(environmentRef).bootstrap()
    }
}
