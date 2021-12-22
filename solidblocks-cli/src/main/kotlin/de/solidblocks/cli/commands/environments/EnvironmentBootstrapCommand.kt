package de.solidblocks.cli.commands.environments

import de.solidblocks.cli.commands.BaseCloudEnvironmentCommand
import de.solidblocks.cloud.SolidblocksAppplicationContext

class EnvironmentBootstrapCommand : BaseCloudEnvironmentCommand(name = "bootstrap", help = "bootstrap a cloud environment") {

    override fun run() {
        val context = SolidblocksAppplicationContext(solidblocksDatabaseUrl)
        context.createCloudProvisioner(cloud, environment).bootstrap()
    }
}
