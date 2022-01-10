package de.solidblocks.cli.commands.environment

import de.solidblocks.cloud.SolidblocksAppplicationContext
import kotlin.system.exitProcess

class EnvironmentRotateSecretsCommand :
    BaseCloudEnvironmentCommand(name = "rotate-secrets", help = "rotate secrets") {

    override fun run() {
        val context = SolidblocksAppplicationContext(solidblocksDatabaseUrl)

        if (!context.verifyEnvironmentReference(environmentRef)) {
            exitProcess(1)
        }

        if (!context.cloudManager.rotateEnvironmentSecrets(environmentRef)) {
            exitProcess(1)
        }
    }
}
