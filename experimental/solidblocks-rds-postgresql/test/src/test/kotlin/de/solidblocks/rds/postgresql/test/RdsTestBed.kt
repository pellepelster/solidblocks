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

    val network = Network.newNetwork()

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
            it.withNetworkAliases(RdsPostgresqlIntegrationTest.backupHost)
            it.withNetwork(network)
            it.withFileSystemBind(storageDir, "/storage/local")
            it.withEnv(
                    mapOf(
                            "MINIO_ADMIN_USER" to "admin12345",
                            "MINIO_ADMIN_PASSWORD" to "admin12345",
                            "MINIO_TLS_PRIVATE_KEY" to RdsPostgresqlIntegrationTest.minioCertificatePrivateBase64,
                            "MINIO_TLS_PUBLIC_KEY" to RdsPostgresqlIntegrationTest.minioCertificatePublicBase64,
                            "BUCKET_SPECS" to "${RdsPostgresqlIntegrationTest.bucket}:${RdsPostgresqlIntegrationTest.accessKey}:${RdsPostgresqlIntegrationTest.secretKey}"
                    )
            )
        }

        container.start()
        logConsumer.waitForLogLine("[solidblocks-minio] provisioning completed")
    }

    fun createAndStartPostgresContainer(storageDir: File, logConsumer: TestContainersLogConsumer) = GenericContainer("solidblocks-rds-postgresql").also {
        it.withLogConsumer(logConsumer)
        it.withNetwork(network)
        it.withExposedPorts(5432)
        it.withFileSystemBind(storageDir.absolutePath, "/storage/local")
        it.withEnv(
                mapOf(
                        "DB_BACKUP_S3_HOST" to RdsPostgresqlIntegrationTest.backupHost,
                        "DB_BACKUP_S3_BUCKET" to RdsPostgresqlIntegrationTest.bucket,
                        "DB_BACKUP_S3_ACCESS_KEY" to RdsPostgresqlIntegrationTest.accessKey,
                        "DB_BACKUP_S3_SECRET_KEY" to RdsPostgresqlIntegrationTest.secretKey,

                        "DB_DATABASE" to RdsPostgresqlIntegrationTest.database,
                        "DB_USERNAME" to RdsPostgresqlIntegrationTest.databaseUser,
                        "DB_PASSWORD" to RdsPostgresqlIntegrationTest.databasePassword,

                        "DB_BACKUP_S3_CA_PUBLIC_KEY" to RdsPostgresqlIntegrationTest.caPublicBase64,
                )
        )
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