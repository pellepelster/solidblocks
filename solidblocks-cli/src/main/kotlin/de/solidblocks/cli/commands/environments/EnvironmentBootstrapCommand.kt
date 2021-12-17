package de.solidblocks.cli.commands.environments

import de.solidblocks.cli.commands.BaseCloudEnvironmentCommand
import de.solidblocks.cloud.CloudManager
import de.solidblocks.cloud.SolidblocksAppplicationContext

class EnvironmentBootstrapCommand : BaseCloudEnvironmentCommand(name = "bootstrap", help = "bootstrap a cloud environment") {

    override fun run() {
        val context = SolidblocksAppplicationContext(solidblocksDatabaseUrl)

        val cloudManager = CloudManager(context.vaultRootClientProvider(cloud, environment), context.createProvisioner(cloud, environment), context.configurationManager)
        cloudManager.bootstrap(cloud, environment)
    }
}
