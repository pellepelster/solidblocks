package de.solidblocks.ingress.agent

import de.solidblocks.agent.base.DockerManager
import de.solidblocks.base.BaseConstants.serviceId
import de.solidblocks.base.EnvironmentServiceReference
import de.solidblocks.base.solidblocksVersion
import de.solidblocks.ingress.agent.config.*
import mu.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeBytes

class CaddyManager(reference: EnvironmentServiceReference, certificatesDir: File, network: String? = null) {

    private val logger = KotlinLogging.logger {}

    private val dockerManager: DockerManager

    // TODO: ensure permissions (use /solidblocks)
    private val tempDir: Path = Files.createTempDirectory("${reference.cloud}-${reference.environment}-${reference.service}")

    private val caddyConfigFile = Path.of(tempDir.toString(), "caddy.json")

    private val CADDY_CONFIG_PATH = "/solidblocks/config/caddy.json"

    private val CADDY_CERTIFICATES_DIR = "/solidblocks/certificates"

    fun writeCaddyConfig(config: CaddyConfig) {
        logger.info { "writing caddy config to '$caddyConfigFile" }
        caddyConfigFile.writeBytes(CaddyConfig.serializeToBytes(config))
    }

    init {

        writeCaddyConfig(CaddyConfig())

        dockerManager = DockerManager(
            "ghcr.io/pellepelster/solidblocks-ingress:${solidblocksVersion()}", reference.service, setOf(80),
            mapOf(
                caddyConfigFile.toString() to CADDY_CONFIG_PATH,
                certificatesDir.toString() to CADDY_CERTIFICATES_DIR,
            ),
            healthCheck = false, network = network
        )
    }

    fun start(): Boolean {
        return dockerManager.start()
    }

    fun stop(): Boolean {
        return dockerManager.stop()
    }

    fun updateServices(services: List<ReverseProxyConfiguration>) {
        val servers = services.map {
            serviceId(it.reference) to Server(
                automaticHttps = AutomaticHttps(disable = true),
                routes = listOf(
                    Route(
                        match = listOf(Match(host = it.hostnames)),
                        handle = listOf(
                            Handler(
                                transport = Transport(
                                    tls =
                                    Tls(
                                        clientCertificateFile = "$CADDY_CERTIFICATES_DIR/${it.clientCertificateFile.toPath().fileName}",
                                        clientCertificateKeyFile = "$CADDY_CERTIFICATES_DIR/${it.clientCertificateKeyFile.toPath().fileName}",
                                        rootCAPemFiles = listOf("$CADDY_CERTIFICATES_DIR/${it.rootCAPemFile.toPath().fileName}")
                                    )
                                ),
                                upstreams = listOf(Upstream(it.upstream))
                            )
                        )
                    )
                )
            )
        }.toMap()

        if (servers.isNotEmpty()) {
            val config = CaddyConfig(apps = mapOf("http" to Http(servers)))
            writeCaddyConfig(config)
        }
    }

    fun httpPort(): String = dockerManager.mappedPort(80)!!
}
