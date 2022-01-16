package de.solidblocks.ingress.agent

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.agent.base.AgentHttpServer
import de.solidblocks.base.BaseConstants.serversDomain
import de.solidblocks.base.resources.EnvironmentServiceResource
import de.solidblocks.vault.VaultCaCertificateManager
import de.solidblocks.vault.VaultCertificateManager
import de.solidblocks.vault.VaultConstants
import de.solidblocks.vault.VaultTokenManager
import mu.KotlinLogging

class RunCommand : CliktCommand(name = "run") {

    private val logger = KotlinLogging.logger {}

    private val vaultAddress by option(envvar = "VAULT_ADDR").required()

    private val vaultToken by option(envvar = "VAULT_TOKEN").required()

    private val cloud by option(envvar = "SOLIDBLOCKS_CLOUD").required()

    private val rootDomain by option(envvar = "SOLIDBLOCKS_ROOT_DOMAIN").required()

    private val environment by option(envvar = "SOLIDBLOCKS_ENVIRONMENT").required()

    override fun run() {

        val reference = EnvironmentServiceResource(cloud, environment, "ingress")

        val vaultTokenManager = VaultTokenManager(vaultAddress, vaultToken)

        val vaultCertificateManager =
            VaultCertificateManager(
                vaultAddress,
                vaultToken,
                pkiMount = VaultConstants.environmentClientPkiMountName(reference),
                commonName = serversDomain(reference, rootDomain)
            )

        val vaultCaCertificateManager =
            VaultCaCertificateManager(
                vaultAddress,
                vaultToken,
                pkiMount = VaultConstants.environmentClientPkiMountName(reference),
            )

        val agentHttpServer = AgentHttpServer(vaultCertificateManager, vaultCaCertificateManager)
        agentHttpServer.waitForShutdown()
    }
}

fun main(args: Array<String>) {
    RunCommand().main(args)
}
