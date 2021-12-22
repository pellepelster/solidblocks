package de.solidblocks.ingress

import de.solidblocks.base.ServiceReference
import io.github.resilience4j.retry.RetryConfig
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

class IngressManager(
    private val reference: ServiceReference,
    private val storageDir: String,
    solidblocksVaultAddress: String,
    solidblocksVaultToken: String,
) {

    private val logger = KotlinLogging.logger {}

    val retryConfig =
        RetryConfig.custom<Boolean>().retryOnResult { it == false }.maxAttempts(20).waitDuration(Duration.ofSeconds(1))

    fun start(): Boolean {
        logger.info { "starting ingress" }

        val tempDir = Files.createTempDirectory("${reference.cloud}-${reference.environment}-${reference.service}")

        val vaultConfigFile = Path.of(tempDir.toString(), "vault-config.json")
        logger.info { "writing vault config to '$vaultConfigFile" }

        // vaultConfigFile.writeBytes(objectMapper.writeValueAsBytes(vaultConfig))

        /*
        val result = dockerClient.createContainerCmd(DOCKER_IMAGE)
                .withExposedPorts(ExposedPort(8200))
                .withLabels(mapOf(SERVICE_LABEL_KEY to "vault"))
                .withHostConfig(
                        HostConfig.newHostConfig()
                                .withPortBindings(PortBinding(bindPort, ExposedPort(8200)))
                                .withBinds(
                                        Bind(storageDir, Volume("/storage/local")),
                                        Bind(vaultConfigFile.toString(), Volume("/solidblocks/config/vault.json"))
                                )
                ).exec()
        dockerClient.startContainerCmd(result.id).exec()
    */
        if (!waitForReady()) return false

        return true
    }

    private fun waitForReady(): Boolean {
        return true
    }
}
