package de.solidblocks.cli.commands.environment.agents

import de.solidblocks.cli.commands.AgentManager
import de.solidblocks.cli.commands.InstanceManager
import de.solidblocks.cli.commands.environment.BaseCloudEnvironmentCommand
import de.solidblocks.cloud.ApplicationContext
import de.solidblocks.provisioner.hetzner.Hetzner
import kotlin.system.exitProcess

class AgentsListCommand :
    BaseCloudEnvironmentCommand(name = "list", help = "list all agents") {

    override fun run() {
        val context = ApplicationContext(solidblocksDatabaseUrl)

        if (!context.verifyEnvironmentReference(environmentRef)) {
            exitProcess(1)
        }

        val instanceManager =
            InstanceManager(Hetzner.createCloudApi(context.repositories.environments.getEnvironment(environmentRef)!!))

        val environment = context.createEnvironmentContext(environmentRef)

        val agentManager = AgentManager(
            instanceManager,
            environment.serverCertificateManager("TODO"),
            environment.serverCaCertificateManager()
        )

        agentManager.listAllAgents().forEach {
            println("${it.name} = ${it.publicIp}")
        }
    }
}
