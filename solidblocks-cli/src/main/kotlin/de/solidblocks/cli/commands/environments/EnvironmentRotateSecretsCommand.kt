package de.solidblocks.cli.commands.environments

import de.solidblocks.cli.cloud.commands.BaseCloudSpringCommand
import de.solidblocks.cloud.config.CloudConfigurationManager
import org.springframework.stereotype.Component

@Component
class EnvironmentRotateSecretsCommand :
    BaseCloudSpringCommand(name = "rotate-secrets", help = "rotate secrets") {

    override fun run() {
        runSpringApplication {
            it.getBean(CloudConfigurationManager::class.java).rotateEnvironmentSecrets(cloud, environment)
        }
    }
}
