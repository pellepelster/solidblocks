package de.solidblocks.test

import de.solidblocks.base.resources.TenantResource
import de.solidblocks.cloud.ApplicationContext
import de.solidblocks.cloud.VaultCloudConfiguration.createEnvironmentVaultConfig
import de.solidblocks.cloud.VaultCloudConfiguration.createTenantVaultConfig
import de.solidblocks.cloud.environments.EnvironmentApplicationContext
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.cloud.services.ServiceProvisioner
import de.solidblocks.cloud.tenants.TenantApplicationContext
import de.solidblocks.provisioner.minio.MinioCredentials
import de.solidblocks.test.TestConstants.TEST_DB_JDBC_URL
import de.solidblocks.vault.EnvironmentVaultManager
import de.solidblocks.vault.agent.VaultService
import mu.KotlinLogging
import org.testcontainers.containers.DockerComposeContainer
import java.io.File
import java.nio.file.Files
import kotlin.io.path.writeBytes

class KDockerComposeContainer(file: File) : DockerComposeContainer<KDockerComposeContainer>(file)

class IntegrationTestEnvironment {

    val vaultRootToken: String
        get() = environment.rootToken!!

    private val logger = KotlinLogging.logger {}

    private val dockerEnvironment: DockerComposeContainer<*>

    val reference = TenantResource("local", "dev", "tenant1")

    val rootDomain = "local.test"

    private lateinit var context: ApplicationContext

    init {
        val dockerComposeContent =
            IntegrationTestEnvironment::class.java.getResource("/local-env/docker-compose.yml").readBytes()

        val tempFile = Files.createTempFile("solidblocks-local-env-dockercompose", ".yml")
        tempFile.writeBytes(dockerComposeContent)

        dockerEnvironment = KDockerComposeContainer(tempFile.toFile())
            .apply {
                withExposedService("vault", 8200)
                withExposedService("minio", 9000)
            }
    }

    val vaultAddress: String
        get() = "http://localhost:${dockerEnvironment.getServicePort("vault", 8200)}"

    val minioAddress: String
        get() = "http://localhost:9000"

    val environment: EnvironmentEntity
        get() =
            context.environmentRepository.getEnvironment(reference)

    val minioCredentialProvider: () -> MinioCredentials
        get() = { MinioCredentials(minioAddress, "admin", "a9776029-2852-4d60-af81-621b91da711d") }

    fun start() {
        dockerEnvironment.start()
        context =
            ApplicationContext(TEST_DB_JDBC_URL(), vaultAddress, minioCredentialProvider, true)
    }

    fun stop() {
        try {
            dockerEnvironment.stop()
        } catch (e: Exception) {
            logger.warn(e) {}
        }
    }

    fun createCloud(): Boolean {
        logger.info { "creating test env (${reference.cloud}/${reference.environment})" }

        context.cloudsManager.createCloud(reference.cloud, rootDomain)
        context.environmentsManager.create(
            reference,
            reference.environment,
            "juergen@test.local",
            "password1",
            "<none>",
            "<none>",
            "<none>",
            "<none>"
        )
        context.tenantsManager.create(reference, reference.tenant, "pelle@pelle.io", "password1")

        val environment = context.environmentRepository.getEnvironment(reference)
        val tenant = context.tenantRepository.getTenant(reference)
        val provisioner = context.createProvisioner(reference)

        provisioner.addResourceGroup(createEnvironmentVaultConfig(emptySet(), environment))
        provisioner.addResourceGroup(createTenantVaultConfig(emptySet(), tenant))

        val result = provisioner.apply()

        if (!result) {
            logger.error { "provisioning test env (${reference.cloud}/${reference.environment}) failed" }
            return false
        }

        val environmentAfterProvisioning = context.environmentRepository.getEnvironment(reference)
        logger.info { "vault is available at '$vaultAddress' with root token '${environmentAfterProvisioning.rootToken}'" }
        logger.info { "minio is available at '$minioAddress' with access key '${minioCredentialProvider.invoke().accessKey}' and secret key '${minioCredentialProvider.invoke().secretKey}'" }

        return result
    }

    fun createVaultService(service: String): Boolean {

        val service = VaultService(
            reference.toService(service),
            context.serviceRepository
        )

        service.createService()

        val provisioner = context.createProvisioner(reference)
        return service.bootstrapService(provisioner)
    }

    fun createService(name: String): String {
        val serviceRef = reference.toService(name)

        val provisioner = context.createProvisioner(reference)

        provisioner.addResourceGroup(ServiceProvisioner.createVaultConfigResourceGroup(serviceRef))
        provisioner.apply()

        val vaultManager = EnvironmentVaultManager(vaultAddress, environment.rootToken!!, reference)
        return vaultManager.createServiceToken(name, serviceRef)
    }

    fun environmentContext(): EnvironmentApplicationContext {
        return EnvironmentApplicationContext(reference, context.environmentRepository, true, vaultAddress)
    }

    fun tenantContext(): TenantApplicationContext {
        return TenantApplicationContext(reference, context.tenantRepository, true, vaultAddress)
    }
}
