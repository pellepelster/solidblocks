package de.solidblocks.cli.commands

import de.solidblocks.agent.base.api.BaseAgentApiClient
import de.solidblocks.base.Waiter
import de.solidblocks.base.solidblocksVersion
import de.solidblocks.cli.commands.environment.agents.RunningInstance
import de.solidblocks.cloud.model.entities.Role
import de.solidblocks.vault.VaultCaCertificateManager
import de.solidblocks.vault.VaultCertificateManager
import mu.KotlinLogging

data class AgentVersionInformation(val instance: RunningInstance, val currentVersion: String?)

class AgentManager(
    private val instanceManager: InstanceManager,
    val vaultCertificateManager: VaultCertificateManager,
    val vaultCaCertificateManager: VaultCaCertificateManager
) {

    val logger = KotlinLogging.logger {}

    fun updateAllAgents(): Boolean {

        val targetVersion = solidblocksVersion()

        logger.info { "calculating agent update" }

        val agentsToUpdate = listAllAgents().map {

            val address = "http://${it.publicIp}:8080"
            val currentVersion =
                BaseAgentApiClient(address, vaultCertificateManager, vaultCaCertificateManager).version()

            if (currentVersion == null) {
                logger.error { "unable to retrieve current agent version for instance '${it.name}' at $address" }
            }
            AgentVersionInformation(it, currentVersion?.version)
        }

        if (agentsToUpdate.any { it.currentVersion.isNullOrBlank() }) {
            return false
        }

        for (agent in agentsToUpdate) {

            if (agent.currentVersion == targetVersion) {
                logger.info { "instance '${agent.instance.name}' is already up to date $targetVersion" }
                continue
            }

            val client = BaseAgentApiClient("http://${agent.instance.publicIp}:8080", vaultCertificateManager, vaultCaCertificateManager)

            if (client.triggerUpdate(targetVersion) != true) {
                logger.error { "failed to trigger update for instance '${agent.instance.name}' at ${client.address}" }
                return false
            }

            logger.info { "update for instance '${agent.instance.name}' successfully triggered" }

            Waiter.defaultWaiter().waitFor {
                val currentVersion = client.version()
                logger.info { "waiting for update on instance '${agent.instance.name}' at ${client.address} to reach version $targetVersion" }
                currentVersion?.version == targetVersion
            }
        }

        return true
    }

    fun listAllAgents() = instanceManager.allServers().filter { it.role == Role.service }
}
