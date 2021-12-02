package de.solidblocks.cli.commands.environments

import de.solidblocks.cli.cloud.commands.BaseCloudSpringCommand
import de.solidblocks.cloud.CloudMananger

class EnvironmentBootstrapCommand : BaseCloudSpringCommand(name = "bootstrap", help = "bootstrap a cloud environment") {

    override fun run() {
        runSpringApplication {
            it.getBean(CloudMananger::class.java).bootstrap(cloud, environment)
        }
    }
}
