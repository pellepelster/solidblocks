package de.solidblocks.test

import de.solidblocks.base.EnvironmentReference
import de.solidblocks.base.ServiceReference
import de.solidblocks.cloud.SolidblocksAppplicationContext
import de.solidblocks.cloud.VaultCloudConfiguration.createVaultConfig
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.provisioner.minio.MinioCredentials
import de.solidblocks.service.vault.VaultService
import de.solidblocks.test.TestConstants.TEST_DB_JDBC_URL
import de.solidblocks.vault.Certificate
import de.solidblocks.vault.VaultCertificateManager
import de.solidblocks.vault.VaultConstants
import mu.KotlinLogging
import org.testcontainers.containers.DockerComposeContainer
import java.io.File
import java.nio.file.Files
import kotlin.io.path.writeBytes

class KDockerComposeContainer(file: File) : DockerComposeContainer<KDockerComposeContainer>(file)

class DevelopmentEnvironment {

    private val logger = KotlinLogging.logger {}

    private val dockerEnvironment: DockerComposeContainer<*>

    private val cloud = "local"

    val rootDomain = "local.test"

    val environment = "dev"

    private val certificateManagers: MutableMap<ServiceReference, VaultCertificateManager> = mutableMapOf()

    private lateinit var applicationContext: SolidblocksAppplicationContext

    val rootToken: String get() = environmentModel.getConfigValue(VaultConstants.ROOT_TOKEN_KEY)

    init {
        val dockerComposeContent =
            DevelopmentEnvironment::class.java.getResource("/local-env/docker-compose.yml").readBytes()

        val tempFile = Files.createTempFile("solidblocks-local-env-dockercompose", ".yml")
        tempFile.writeBytes(dockerComposeContent)

        dockerEnvironment = KDockerComposeContainer(tempFile.toFile())
            .apply {
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

    val environmentModel: EnvironmentEntity
        get() =
            applicationContext.environmentRepository.getEnvironment(cloud, environment)

    val minioCredentialProvider: () -> MinioCredentials
        get() = { MinioCredentials(minioAddress, "admin", "a9776029-2852-4d60-af81-621b91da711d") }

    fun start() {
        dockerEnvironment.start()
        applicationContext = SolidblocksAppplicationContext(TEST_DB_JDBC_URL(), vaultAddress, minioCredentialProvider)
    }

    fun stop() {
        try {
            dockerEnvironment.stop()
        } catch (e: Exception) {
            logger.warn(e) {}
        }
    }

    fun createCloud(): Boolean {
        logger.info { "creating test env ($cloud/$environment)" }

        applicationContext.cloudManager.createCloud(cloud, rootDomain, true)
        applicationContext.cloudManager.createEnvironment(
            cloud, environment,
            "<none>",
            "<none>",
            "<none>",
            "<none>"
        )

        val environment = applicationContext.environmentRepository.getEnvironment(cloud, environment)
        val provisioner = applicationContext.createProvisioner(cloud, this.environment)

        provisioner.addResourceGroup(createVaultConfig(emptySet(), environment))

        val result = provisioner.apply()

        if (!result) {
            logger.error { "provisioning test env ($cloud/$environment) failed" }
            return false
        }

        logger.info { "vault is available at '$vaultAddress' with root token '$rootToken'" }
        logger.info { "minio is available at '$minioAddress' with access key '${minioCredentialProvider.invoke().accessKey}' and secret key '${minioCredentialProvider.invoke().secretKey}'" }

        return result
    }

    fun createVaultService(service: String): Boolean {

        val service = VaultService(
            ServiceReference(cloud, environment, service),
            applicationContext.serviceRepository
        )

        service.createService()

        val provisioner = applicationContext.createProvisioner(cloud, this.environment)
        service.bootstrapService(provisioner)

        return true
    }

    fun createCertificate(reference: ServiceReference): Certificate? {
        certificateManagers[reference] = VaultCertificateManager(vaultAddress, rootToken, reference, rootDomain, true)
        return certificateManagers[reference]!!.issueCertificate()
    }
}
