package de.solidblocks.ingress.agent

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.agent.base.AgentHttpServer
import de.solidblocks.base.EnvironmentServiceReference
import de.solidblocks.vault.VaultCaCertificateManager
import de.solidblocks.vault.VaultCertificateManager
import de.solidblocks.vault.VaultConstants.environmentServerPkiMountName
import de.solidblocks.vault.VaultConstants.serversDomain
import de.solidblocks.vault.VaultTokenManager
import mu.KotlinLogging

class RunCommand : CliktCommand(name = "run") {

    private val logger = KotlinLogging.logger {}

    val vaultAddress by option(envvar = "VAULT_ADDR").required()

    val vaultToken by option(envvar = "VAULT_TOKEN").required()

    val cloud by option(envvar = "SOLIDBLOCKS_CLOUD").required()

    val rootDomain by option(envvar = "SOLIDBLOCKS_ROOT_DOMAIN").required()

    val environment by option(envvar = "SOLIDBLOCKS_ENVIRONMENT").required()

    override fun run() {

        val reference = EnvironmentServiceReference(cloud, environment, "ingress")

        val vaultTokenManager = VaultTokenManager(vaultAddress, vaultToken)
        val vaultCertificateManager =
            VaultCertificateManager(
                vaultAddress, vaultToken, pkiMount = environmentServerPkiMountName(reference),
                commonName = serversDomain(reference, rootDomain)
            )

        val vaultCaCertificateManager =
            VaultCaCertificateManager(
                vaultAddress, vaultToken, pkiMount = environmentServerPkiMountName(reference),
            )

        val agentHttpServer = AgentHttpServer(vaultCertificateManager, vaultCaCertificateManager)
        agentHttpServer.waitForShutdown()

        /*
        val reference = ServiceReference("xxx", "yyy", "service1")
        val service = VaultServiceManager(reference, "/storage/local", VaultManager("xx", "xx", reference.environmentReference))
        service.start()
        */
    }
}

fun main(args: Array<String>) {
    RunCommand().main(args)
}
