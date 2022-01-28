package de.solidblocks.cli.commands.environment

import de.solidblocks.cloud.ApplicationContext
import kotlin.system.exitProcess

class EnvironmentRotateSecretsCommand :
    BaseCloudEnvironmentCommand(name = "rotate-secrets", help = "rotate secrets") {

    override fun run() {
        val context = ApplicationContext(solidblocksDatabaseUrl)

        if (!context.managers.environments.verifyReference(environmentRef)) {
            exitProcess(1)
        }

        if (!context.managers.clouds.rotateEnvironmentSecrets(environmentRef)) {
            exitProcess(1)
        }
    }
}
