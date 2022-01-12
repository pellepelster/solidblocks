package de.solidblocks.ingress.agent

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import de.solidblocks.agent.base.AgentHttpServer
import de.solidblocks.base.EnvironmentServiceReference
import de.solidblocks.vault.EnvironmentVaultCertificateManager
import de.solidblocks.vault.VaultTokenManager
import mu.KotlinLogging

class RunCommand : CliktCommand(name = "run") {

    private val logger = KotlinLogging.logger {}

    val vaultAddress by option(envvar = "VAULT_ADDR")

    val vaultToken by option(envvar = "VAULT_TOKEN")

    val cloud by option(envvar = "SOLIDBLOCKS_CLOUD")

    val rootDomain by option(envvar = "SOLIDBLOCKS_ROOT_DOMAIN")

    val environment by option(envvar = "SOLIDBLOCKS_ENVIRONMENT")

    override fun run() {

        val reference = EnvironmentServiceReference(cloud!!, environment!!, "ingress")

        val vaultTokenManager = VaultTokenManager(vaultAddress!!, vaultToken!!)
        val vaultCertificateManager =
            EnvironmentVaultCertificateManager(vaultAddress!!, vaultToken!!, reference, rootDomain!!)

        val agentHttpServer = AgentHttpServer()
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
