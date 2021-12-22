package de.solidblocks.test

import de.solidblocks.api.resources.ResourceGroup
import de.solidblocks.base.EnvironmentReference
import de.solidblocks.base.ServiceReference
import de.solidblocks.cloud.SolidblocksAppplicationContext
import de.solidblocks.cloud.VaultCloudConfiguration.createVaultConfig
import de.solidblocks.cloud.model.ModelConstants.CONSUL_MASTER_TOKEN_KEY
import de.solidblocks.cloud.model.ModelConstants.CONSUL_SECRET_KEY
import de.solidblocks.cloud.model.ModelConstants.GITHUB_TOKEN_RO_KEY
import de.solidblocks.cloud.model.ModelConstants.GITHUB_USERNAME_KEY
import de.solidblocks.cloud.model.ModelConstants.HETZNER_CLOUD_API_TOKEN_RO_KEY
import de.solidblocks.cloud.model.ModelConstants.HETZNER_CLOUD_API_TOKEN_RW_KEY
import de.solidblocks.cloud.model.ModelConstants.HETZNER_DNS_API_TOKEN_RW_KEY
import de.solidblocks.cloud.model.ModelConstants.serviceId
import de.solidblocks.cloud.model.model.EnvironmentModel
import de.solidblocks.provisioner.minio.MinioCredentials
import de.solidblocks.provisioner.minio.bucket.MinioBucket
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

    val rootToken: String get() = environmentModel.getConfigValue(VaultConstants.ROOT_TOKEN_KEY)

    init {
        val dockerComposeContent =
            SolidblocksLocalEnv::class.java.getResource("/local-env/docker-compose.yml").readBytes()

        val tempFile = Files.createTempFile("solidblocks-local-env-dockercompose", ".yml")
        tempFile.writeBytes(dockerComposeContent)

        dockerEnvironment = KDockerComposeContainer(tempFile.toFile())
            .apply {
                withPull(true)
                withExposedService("vault", 8200)
                withExposedService("minio", 9000)
            }
    }

    val reference: EnvironmentReference
        get() = EnvironmentReference(cloud, environment)

    val vaultAddress: String
        get() = "http://localhost:${dockerEnvironment.getServicePort("vault", 8200)}"

    val minioAddress: String
        get() = "http://localhost:${dockerEnvironment.getServicePort("minio", 9000)}"

    val environmentModel: EnvironmentModel
        get() =
            appplicationContext.environmentRepository.getEnvironment(cloud, environment)

    val minioCredentialProvider: () -> MinioCredentials
        get() = { MinioCredentials(minioAddress, "admin", "a9776029-2852-4d60-af81-621b91da711d") }

    fun start() {
        dockerEnvironment.start()
        appplicationContext = SolidblocksAppplicationContext(TEST_DB_JDBC_URL(), vaultAddress, minioCredentialProvider)
    }

    fun stop() {
        dockerEnvironment.stop()
    }

    fun bootstrap(): Boolean {
        logger.info { "bootstrapping test env" }

        appplicationContext.cloudRepository.let {
            it.createCloud(cloud, "test.env", emptyList())
        }

        appplicationContext.environmentRepository.let {
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

        val environmentConfiguration = appplicationContext.environmentRepository.getEnvironment(cloud, environment)
        val provisioner = appplicationContext.createProvisioner(cloud, environment)

        provisioner.addResourceGroup(createVaultConfig(emptySet(), environmentConfiguration))

        return provisioner.apply()
    }

    fun bootstrapService(reference: ServiceReference): String {
        val provisioner = appplicationContext.createProvisioner(cloud, environment)

        val group = ResourceGroup("${serviceId(reference)}-backup")

        val bucket = MinioBucket(serviceId(reference))
        group.addResource(bucket)

        provisioner.apply()

        return provisioner.lookup(bucket).result!!.name
    }

    fun createServiceReference(service: String) = ServiceReference(cloud, environment, service)
}
