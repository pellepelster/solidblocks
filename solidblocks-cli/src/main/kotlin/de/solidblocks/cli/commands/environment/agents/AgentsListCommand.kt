package de.solidblocks.cli.commands.environment.agents

import de.solidblocks.cli.commands.AgentManager
import de.solidblocks.cli.commands.InstanceManager
import de.solidblocks.cli.commands.environment.BaseCloudEnvironmentCommand
import de.solidblocks.cloud.SolidblocksAppplicationContext
import de.solidblocks.provisioner.hetzner.Hetzner
import kotlin.system.exitProcess

class AgentsListCommand :
    BaseCloudEnvironmentCommand(name = "list", help = "list all agents") {

    override fun run() {
        val context = SolidblocksAppplicationContext(solidblocksDatabaseUrl)

        if (!context.verifyEnvironmentReference(environmentRef)) {
            exitProcess(1)
        }

        val instanceManager = InstanceManager(Hetzner.createCloudApi(context.environmentRepository.getEnvironment(environmentRef)))
        val agentManager = AgentManager(instanceManager)

        agentManager.listAllAgents().forEach {
            println("${it.name} = ${it.publicIp}")
        }
    }
}
