package de.solidblocks.cli.commands.environment

import de.solidblocks.cli.commands.CommandApplicationContext
import kotlin.system.exitProcess

class EnvironmentRotateSecretsCommand :
    BaseCloudEnvironmentCommand(name = "rotate-secrets", help = "rotate secrets") {

    override fun run() {
        val context = CommandApplicationContext(solidblocksDatabaseUrl)

        if (!context.managers.environments.verifyReference(environmentRef)) {
            exitProcess(1)
        }

        if (!context.managers.clouds.rotateEnvironmentSecrets(environmentRef)) {
            exitProcess(1)
        }
    }
}
