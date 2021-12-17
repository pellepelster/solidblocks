package de.solidblocks.test

import de.solidblocks.base.EnvironmentReference
import de.solidblocks.cloud.SolidblocksAppplicationContext
import de.solidblocks.cloud.VaultConfig.Companion.createVaultConfig
import de.solidblocks.cloud.config.ConfigConstants.CONSUL_MASTER_TOKEN_KEY
import de.solidblocks.cloud.config.ConfigConstants.CONSUL_SECRET_KEY
import de.solidblocks.cloud.config.ConfigConstants.GITHUB_TOKEN_RO_KEY
import de.solidblocks.cloud.config.ConfigConstants.GITHUB_USERNAME_KEY
import de.solidblocks.cloud.config.ConfigConstants.HETZNER_CLOUD_API_TOKEN_RO_KEY
import de.solidblocks.cloud.config.ConfigConstants.HETZNER_CLOUD_API_TOKEN_RW_KEY
import de.solidblocks.cloud.config.ConfigConstants.HETZNER_DNS_API_TOKEN_RW_KEY
import de.solidblocks.cloud.config.model.CloudEnvironmentConfiguration
import de.solidblocks.test.TestConstants.TEST_DB_JDBC_URL
import de.solidblocks.vault.VaultConstants
import mu.KotlinLogging
import org.testcontainers.containers.DockerComposeContainer
import java.io.File
import java.nio.file.Files
import kotlin.io.path.writeBytes

class KDockerComposeContainer(file: File) : DockerComposeContainer<KDockerComposeContainer>(file)

class SolidblocksLocalEnv {

    private val logger = KotlinLogging.logger {}

    private val dockerEnvironment: DockerComposeContainer<*>

    val cloud = "local"

    val environment = "env1"

    lateinit var appplicationContext: SolidblocksAppplicationContext

    val rootToken: String get() = environmentConfiguration.getConfigValue(VaultConstants.ROOT_TOKEN_KEY)

    init {
        val dockerComposeContent = SolidblocksLocalEnv::class.java.getResource("/local-env/docker-compose.yml").readBytes()

        val tempFile = Files.createTempFile("SolidblocksLocalEnv", "dockercompose")
        tempFile.writeBytes(dockerComposeContent)

        dockerEnvironment = KDockerComposeContainer(tempFile.toFile())
            .apply {
                withPull(true)
                withExposedService("vault", 8200)
                withExposedService("backup", 9001)
            }
    }

    val reference: EnvironmentReference
        get() = EnvironmentReference(cloud, environment)

    val vaultAddress: String
        get() = "http://localhost:${dockerEnvironment.getServicePort("vault", 8200)}"

    fun backupAddress() = "http://localhost:${dockerEnvironment.getServicePort("backup", 9001)}"

    fun start() {
        dockerEnvironment.start()
        appplicationContext = SolidblocksAppplicationContext(TEST_DB_JDBC_URL, vaultAddress)
    }

    fun stop() {
        dockerEnvironment.stop()
    }

    fun bootstrap(): Boolean {
        logger.info { "bootstrapping test env" }

        appplicationContext.configurationManager.let {

            it.createCloud(cloud, "test.env", emptyList())
            it.createEnvironment(cloud, environment)
            it.updateEnvironment(
                cloud, environment,
                mapOf(
                    CONSUL_SECRET_KEY to "<none>",
                    CONSUL_MASTER_TOKEN_KEY to "<none>",
                    GITHUB_TOKEN_RO_KEY to "<none>",
                    GITHUB_USERNAME_KEY to "<none>",
                    HETZNER_DNS_API_TOKEN_RW_KEY to "<none>",
                    HETZNER_CLOUD_API_TOKEN_RW_KEY to "<none>",
                    HETZNER_CLOUD_API_TOKEN_RO_KEY to "<none>",
                )
            )
        }

        val environmentConfiguration = appplicationContext.environmentConfiguration(cloud, environment)
        val provisioner = appplicationContext.createProvisioner(cloud, environment)

        provisioner.addResourceGroup(createVaultConfig(emptySet(), environmentConfiguration))

        return provisioner.apply()
    }

    val environmentConfiguration: CloudEnvironmentConfiguration
        get() =
            appplicationContext.environmentConfiguration(cloud, environment)
}
