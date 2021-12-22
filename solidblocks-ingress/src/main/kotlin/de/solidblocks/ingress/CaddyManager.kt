package de.solidblocks.ingress

import de.solidblocks.base.ServiceReference
import de.solidblocks.ingress.config.CaddyConfig
import de.solidblocks.service.base.DockerManager
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeBytes

class CaddyManager(
    private val reference: ServiceReference,
    private val storageDir: String,
) {

    private val logger = KotlinLogging.logger {}

    private val dockerManager: DockerManager

    private val tempDir: Path

    init {

        // TODO: ensure permissions
        tempDir = Files.createTempDirectory("${reference.cloud}-${reference.environment}-${reference.service}")

        val caddyConfigFile = Path.of(tempDir.toString(), "caddy.json")
        logger.info { "writing caddy config to '$caddyConfigFile" }
        caddyConfigFile.writeBytes(CaddyConfig.serializeToBytes(CaddyConfig()))

        dockerManager = DockerManager(
            "solidblocks-ingress",
            reference.service,
            storageDir,
            setOf(2019),
            mapOf(caddyConfigFile.toString() to "/solidblocks/config/caddy.json"),
            healthPath = "/config"
        )
    }

    fun start(): Boolean {
        return dockerManager.start()
    }

    fun stop(): Boolean {
        return dockerManager.stop()
    }

    fun test() {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("http://localhost:${dockerManager.mappedPort(2019)}/config")
            .build()

        val result = client.newCall(request).execute()
        result.toString()
    }
}
