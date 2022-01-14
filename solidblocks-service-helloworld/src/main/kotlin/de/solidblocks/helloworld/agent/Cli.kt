package de.solidblocks.helloworld.agent

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import de.solidblocks.agent.base.AgentHttpServer
import de.solidblocks.base.ServiceReference
import de.solidblocks.vault.VaultCaCertificateManager
import de.solidblocks.vault.VaultCertificateManager
import de.solidblocks.vault.VaultConstants.environmentClientPkiMountName
import de.solidblocks.vault.VaultConstants.serversDomain
import de.solidblocks.vault.VaultConstants.tenantServerPkiMountName
import de.solidblocks.vault.VaultTokenManager
import mu.KotlinLogging

class RunCommand : CliktCommand(name = "run") {

    private val vaultAddress by option(envvar = "VAULT_ADDR").required()

    private val vaultToken by option(envvar = "VAULT_TOKEN").required()

    private val cloud by option(envvar = "SOLIDBLOCKS_CLOUD").required()

    private val rootDomain by option(envvar = "SOLIDBLOCKS_ROOT_DOMAIN").required()

    private val environment by option(envvar = "SOLIDBLOCKS_ENVIRONMENT").required()

    private val tenant by option(envvar = "SOLIDBLOCKS_TENANT").required()

    private val service by option(envvar = "SOLIDBLOCKS_SERVICE").required()

    val altNames by option("--alt-names", envvar = "SOLIDBLOCKS_CERTIFICATE_ALT_NAMES").split(",").default(emptyList())

    private val logger = KotlinLogging.logger {}

    override fun run() {

        val reference = ServiceReference(cloud, environment, tenant, service)

        val vaultTokenManager = VaultTokenManager(vaultAddress, vaultToken)

        val vaultCertificateManager =
            VaultCertificateManager(
                vaultAddress,
                vaultToken,
                altNames = altNames,
                pkiMount = tenantServerPkiMountName(reference),
                commonName = serversDomain(reference, rootDomain)
            )

        val vaultCaCertificateManager =
            VaultCaCertificateManager(
                vaultAddress,
                vaultToken,
                pkiMount = environmentClientPkiMountName(reference),
            )

        val agentHttpServer = AgentHttpServer(vaultCertificateManager, vaultCaCertificateManager)
        agentHttpServer.waitForShutdown()
    }
}

fun main(args: Array<String>) {
    RunCommand().main(args)
}
