package de.solidblocks.ingress

import de.solidblocks.base.ServiceReference
import de.solidblocks.ingress.config.AutomaticHttps
import de.solidblocks.ingress.config.CaddyConfig
import de.solidblocks.ingress.config.Handler
import de.solidblocks.ingress.config.Http
import de.solidblocks.ingress.config.Match
import de.solidblocks.ingress.config.Route
import de.solidblocks.ingress.config.Server
import de.solidblocks.ingress.config.Tls
import de.solidblocks.ingress.config.Transport
import de.solidblocks.ingress.config.Upstream
import de.solidblocks.service.base.DockerManager
import mu.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeBytes

class CaddyManager(
    reference: ServiceReference,
    storageDir: String,
    caFile: File,
    network: String? = null
) {

    private val logger = KotlinLogging.logger {}

    private val dockerManager: DockerManager

    // TODO: ensure permissions (use /solidblocks)
    private val tempDir: Path =
        Files.createTempDirectory("${reference.cloud}-${reference.environment}-${reference.service}")

    private val caddyConfigFile = Path.of(tempDir.toString(), "caddy.json")

    private val CA_CERT_PATH = "/solidblocks/certificates/ca.crt"

    private val CADDY_CONFIG_PATH = "/solidblocks/config/caddy.json"

    fun writeCaddyConfig(config: CaddyConfig) {
        logger.info { "writing caddy config to '$caddyConfigFile" }
        caddyConfigFile.writeBytes(CaddyConfig.serializeToBytes(config))
    }

    init {

        writeCaddyConfig(CaddyConfig())

        dockerManager = DockerManager(
            "solidblocks-ingress",
            reference.service,
            storageDir,
            setOf(80),
            mapOf(
                caddyConfigFile.toString() to CADDY_CONFIG_PATH,
                caFile.toString() to CA_CERT_PATH
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
                                                        rootCAPemFiles = listOf(CA_CERT_PATH)
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
