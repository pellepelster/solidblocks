package de.solidblocks.rds.postgresql.test

import mu.KotlinLogging
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import java.io.File

class RdsTestBed : AfterEachCallback {

    private val logger = KotlinLogging.logger {}

    private val network = Network.newNetwork()

    private val containers = mutableListOf<GenericContainer<out GenericContainer<*>>>()


    fun imageVersion(image: String): String {
        if (System.getenv("VERSION") != null) {
            return "${image}:${System.getenv("VERSION")}"
        }

        return image
    }

    fun createContainer(dockerImageName: String): GenericContainer<out GenericContainer<*>> {
        val container = GenericContainer(imageVersion(dockerImageName))
        containers.add(container)
        return container
    }

    fun createAndStartMinioContainer() {
        val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))

        val storageDir = initWorldReadableTempDir().absolutePath

        logger.info { "starting minio instance with storage dir '${storageDir}'" }
        val container = createContainer(imageVersion("solidblocks-minio")).also {
            it.withLogConsumer(logConsumer)
            it.withNetworkAliases(RdsPostgresqlMinioBackupIntegrationTest.backupHost)
            it.withNetwork(network)
            it.withFileSystemBind(storageDir, "/storage/data")
            it.withEnv(
                    mapOf(
                            "MINIO_ADMIN_USER" to "admin12345",
                            "MINIO_ADMIN_PASSWORD" to "admin12345",
                            "MINIO_TLS_PRIVATE_KEY" to RdsPostgresqlMinioBackupIntegrationTest.minioCertificatePrivateBase64,
                            "MINIO_TLS_PUBLIC_KEY" to RdsPostgresqlMinioBackupIntegrationTest.minioCertificatePublicBase64,
                            "BUCKET_SPECS" to "${RdsPostgresqlMinioBackupIntegrationTest.bucket}:${RdsPostgresqlMinioBackupIntegrationTest.accessKey}:${RdsPostgresqlMinioBackupIntegrationTest.secretKey}"
                    )
            )
        }

        container.start()
        logConsumer.waitForLogLine("[solidblocks-minio] provisioning completed")
    }

    fun createAndStartPostgresContainer(
        environment: Map<String, String>,
        storageDir: File,
        logConsumer: TestContainersLogConsumer,
        password: String = RdsPostgresqlMinioBackupIntegrationTest.databasePassword,
        customizer: (input: GenericContainer<out GenericContainer<*>>) -> Unit = {}
    ) = GenericContainer("solidblocks-rds-postgresql").also {
        it.withLogConsumer(logConsumer)
        it.withNetwork(network)
        it.withExposedPorts(5432)
        it.withFileSystemBind(storageDir.absolutePath, "/storage/data")
        it.withEnv(
                mapOf(
                        "DB_DATABASE" to RdsPostgresqlMinioBackupIntegrationTest.database,
                        "DB_USERNAME" to RdsPostgresqlMinioBackupIntegrationTest.databaseUser,
                        "DB_PASSWORD" to password,
                ) + environment
        )
        customizer.invoke(it)
        it.start()
    }


    override fun afterEach(context: ExtensionContext?) {

        val client = DockerClientFactory.instance().client()

        containers.forEach {
            logger.info { "killing container '${it.containerId}'" }
            client.killContainerCmd(it.containerId).exec()

            logger.info { "removing container '${it.containerId}'" }
            client.removeContainerCmd(it.containerId).withForce(true).withRemoveVolumes(true).exec()
        }
    }

}