package de.solidblocks.cli.commands.environments

import de.solidblocks.cli.commands.BaseCloudEnvironmentCommand
import de.solidblocks.cloud.SolidblocksAppplicationContext

class EnvironmentRotateSecretsCommand :
    BaseCloudEnvironmentCommand(name = "rotate-secrets", help = "rotate secrets") {

    override fun run() {
        val context = SolidblocksAppplicationContext(solidblocksDatabaseUrl)
        context.configurationManager.rotateEnvironmentSecrets(cloud, environment)
    }
}
