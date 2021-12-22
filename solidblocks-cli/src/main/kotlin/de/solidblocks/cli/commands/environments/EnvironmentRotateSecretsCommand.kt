package de.solidblocks.cli.commands.environments

import de.solidblocks.cli.commands.BaseCloudEnvironmentCommand
import de.solidblocks.cloud.SolidblocksAppplicationContext
import kotlin.system.exitProcess

class EnvironmentRotateSecretsCommand :
    BaseCloudEnvironmentCommand(name = "rotate-secrets", help = "rotate secrets") {

    override fun run() {
        val context = SolidblocksAppplicationContext(solidblocksDatabaseUrl)
        if (!context.cloudManager.rotateEnvironmentSecrets(cloud, environment)) {
            exitProcess(1)
        }
    }
}
