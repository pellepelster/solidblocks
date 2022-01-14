package de.solidblocks.ingress.agent

import de.solidblocks.agent.base.DockerManager
import de.solidblocks.base.ServiceReference
import de.solidblocks.base.solidblocksVersion
import de.solidblocks.ingress.agent.config.*
import mu.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeBytes

class CaddyManager(
    reference: ServiceReference,
    storageDir: String,
    serverCaFile: File,
    clientCert: File,
    clientKey: File,
    network: String? = null
) {

    private val logger = KotlinLogging.logger {}

    private val dockerManager: DockerManager

    // TODO: ensure permissions (use /solidblocks)
    private val tempDir: Path =
        Files.createTempDirectory("${reference.cloud}-${reference.environment}-${reference.service}")

    private val caddyConfigFile = Path.of(tempDir.toString(), "caddy.json")

    private val CADDY_CONFIG_PATH = "/solidblocks/config/caddy.json"

    private val SERVER_CA_CERT_PATH = "/solidblocks/certificates/server_ca.crt"
    private val CLIENT_CERTIFICATE_PATH = "/solidblocks/certificates/client.crt"
    private val CLIENT_KEY_PATH = "/solidblocks/certificates/client.key"

    fun writeCaddyConfig(config: CaddyConfig) {
        logger.info { "writing caddy config to '$caddyConfigFile" }
        caddyConfigFile.writeBytes(CaddyConfig.serializeToBytes(config))
    }

    init {

        writeCaddyConfig(CaddyConfig())

        dockerManager = DockerManager(
            "ghcr.io/pellepelster/solidblocks-ingress:${solidblocksVersion()}",
            reference.service,
            storageDir,
            setOf(80),
            mapOf(
                caddyConfigFile.toString() to CADDY_CONFIG_PATH,
                serverCaFile.toString() to SERVER_CA_CERT_PATH,
                clientCert.toString() to CLIENT_CERTIFICATE_PATH,
                clientKey.toString() to CLIENT_KEY_PATH
            ),
            healthCheck = false,
            network = network
        )
    }

    fun start(): Boolean {
        return dockerManager.start()
    }

    fun stop(): Boolean {
        return dockerManager.stop()
    }

    fun createReverseProxy(upstream: String) {

        val config = CaddyConfig(
            apps = mapOf(
                "http" to
                    Http(
                        servers = mapOf(
                            "server1" to Server(
                                automaticHttps = AutomaticHttps(disable = true),
                                routes = listOf(
                                    Route(
                                        match = listOf(
                                            Match(host = listOf("localhost"))
                                        ),
                                        handle = listOf(
                                            Handler(
                                                transport = Transport(
                                                    tls = Tls(
                                                        clientCertificateFile = CLIENT_CERTIFICATE_PATH,
                                                        clientCertificateKeyFile = CLIENT_KEY_PATH,
                                                        rootCAPemFiles = listOf(SERVER_CA_CERT_PATH)
                                                    )
                                                ),
                                                upstreams = listOf(Upstream(upstream))
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
            )
        )

        writeCaddyConfig(config)
    }

    fun httpPort(): String = dockerManager.mappedPort(80)!!
}
