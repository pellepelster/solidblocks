package de.solidblocks.cli.commands.environment

import de.solidblocks.cli.commands.AgentManager
import de.solidblocks.cli.commands.InstanceManager
import de.solidblocks.cloud.SolidblocksAppplicationContext
import de.solidblocks.cloud.model.entities.Role
import de.solidblocks.provisioner.hetzner.Hetzner
import kotlin.system.exitProcess

data class RunningInstance(val name: String, val publicIp: String, val role: Role)

class EnvironmentAgentsCommand :
    BaseCloudEnvironmentCommand(name = "agents", help = "list all agents") {

    override fun run() {
        val context = SolidblocksAppplicationContext(solidblocksDatabaseUrl)

        if (!context.verifyEnvironmentReference(environmentRef)) {
            exitProcess(1)
        }

        val instanceManager = InstanceManager(Hetzner.createCloudApi(context.environmentRepository.getEnvironment(environmentRef)))
        val agentManager = AgentManager(instanceManager)

        agentManager.updateAllAgents()
    }
}
