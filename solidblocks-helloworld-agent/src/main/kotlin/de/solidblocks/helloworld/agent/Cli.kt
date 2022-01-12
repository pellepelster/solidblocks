package de.solidblocks.helloworld.agent

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.agent.base.AgentHttpServer
import de.solidblocks.base.ServiceReference
import de.solidblocks.vault.ServiceVaultCertificateManager
import de.solidblocks.vault.VaultTokenManager
import mu.KotlinLogging

class RunCommand : CliktCommand(name = "run") {

    val vaultAddress by option(envvar = "VAULT_ADDR").required()

    val vaultToken by option(envvar = "VAULT_TOKEN").required()

    val cloud by option(envvar = "SOLIDBLOCKS_CLOUD").required()

    val rootDomain by option(envvar = "SOLIDBLOCKS_ROOT_DOMAIN").required()

    val environment by option(envvar = "SOLIDBLOCKS_ENVIRONMENT").required()

    val tenant by option(envvar = "SOLIDBLOCKS_TENANT").required()

    val service by option(envvar = "SOLIDBLOCKS_SERVICE").required()

    private val logger = KotlinLogging.logger {}

    override fun run() {

        val reference = ServiceReference(cloud, environment, tenant, service)

        val vaultTokenManager = VaultTokenManager(vaultAddress, vaultToken)
        val vaultCertificateManager =
            ServiceVaultCertificateManager(vaultAddress, vaultToken, reference, rootDomain)

        val agentHttpServer = AgentHttpServer()
        agentHttpServer.waitForShutdown()
    }
}

fun main(args: Array<String>) {
    RunCommand().main(args)
}
