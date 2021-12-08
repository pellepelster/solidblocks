package de.solidblocks.cli.commands.environments

import de.solidblocks.cli.commands.BaseCloudEnvironmentCommand
import de.solidblocks.cloud.config.CloudConfigurationManager

class EnvironmentRotateSecretsCommand :
    BaseCloudEnvironmentCommand(name = "rotate-secrets", help = "rotate secrets") {

    override fun run() {
        val configurationManager = CloudConfigurationManager(solidblocksDatabase().dsl)
        configurationManager.rotateEnvironmentSecrets(cloud, environment)
    }
}
