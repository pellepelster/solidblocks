package de.solidblocks.rds.postgresql.test.extensions

import de.solidblocks.rds.postgresql.test.RdsPostgresqlMinioBackupIntegrationTest
import de.solidblocks.rds.postgresql.test.RdsPostgresqlMinioBackupIntegrationTest.Companion.database
import de.solidblocks.rds.postgresql.test.RdsPostgresqlMinioBackupIntegrationTest.Companion.databasePassword
import de.solidblocks.rds.postgresql.test.RdsPostgresqlMinioBackupIntegrationTest.Companion.databaseUser
import mu.KotlinLogging
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.images.PullPolicy
import java.io.File
import java.time.Duration

class RdsTestBed : AfterEachCallback, AfterAllCallback {

    private val logger = KotlinLogging.logger {}

    private val network = Network.newNetwork()

    private val containers = mutableListOf<GenericContainer<out GenericContainer<*>>>()

    val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))

    private fun createContainer(dockerImageName: String): GenericContainer<out GenericContainer<*>> {
        val container = GenericContainer(dockerImageName)
        containers.add(container)
        return container
    }

    fun createAndStartMinioContainer(): GenericContainer<out GenericContainer<*>> {
        val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))

        val storageDir = initWorldReadableTempDir().absolutePath

        logger.info { "starting minio instance with storage dir '${storageDir}'" }
        val container =
            createContainer("ghcr.io/pellepelster/solidblocks-minio:${System.getenv("VERSION") ?: "snapshot"}-rc").also {
                it.withLogConsumer(logConsumer)
                it.withNetworkAliases(RdsPostgresqlMinioBackupIntegrationTest.backupHost)
                it.withNetwork(network)
                it.withExposedPorts(443)
                it.withFileSystemBind(
                    RdsPostgresqlMinioBackupIntegrationTest::class.java.getResource("/test-dump-backup-file-headers.sh").file,
                    "/test-dump-backup-file-headers.sh",
                    BindMode.READ_ONLY
                )
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

        return container
    }

    fun createAndStartPostgresContainer(
        postgresVersion: Int,
        environment: Map<String, String>,
        storageDir: File,
        password: String = databasePassword,
        customizer: (input: GenericContainer<out GenericContainer<*>>) -> Unit = {}
    ) = createAndStartPostgresContainer(
        "ghcr.io/pellepelster/solidblocks-rds-postgresql:${postgresVersion}-${
            System.getenv("VERSION") ?: "snapshot"
        }-rc", environment, storageDir, password, customizer
    )

    fun createAndStartPostgresContainer(
        imageName: String,
        environment: Map<String, String>,
        storageDir: File,
        password: String = databasePassword,
        customizer: (input: GenericContainer<out GenericContainer<*>>) -> Unit = {}
    ): GenericContainer<out GenericContainer<*>> {

        return GenericContainer(imageName).also {
            it.withLogConsumer(logConsumer)
            it.withImagePullPolicy(PullPolicy.alwaysPull())
            it.withNetwork(network)
            it.withExposedPorts(5432)
            it.withFileSystemBind(storageDir.absolutePath, "/storage/data")
            it.withStartupTimeout(Duration.ofSeconds(300))
            it.withFileSystemBind(
                RdsPostgresqlMinioBackupIntegrationTest::class.java.getResource("/test-dump-backup-file-headers.sh").file,
                "/test-dump-backup-file-headers.sh",
                BindMode.READ_ONLY
            )
            it.withEnv(
                mapOf(
                    "DB_INSTANCE_NAME" to database,
                    "DB_DATABASE_$database" to database,
                    "DB_USERNAME_$database" to databaseUser,
                    "DB_PASSWORD_$database" to password,
                ) + environment
            )
            customizer.invoke(it)
            logger.info { "starting postgres container" }
            it.start()
        }
    }


    override fun afterEach(context: ExtensionContext?) {

        val client = DockerClientFactory.instance().client()

        containers.forEach {
            logger.info { "killing container '${it.containerId}'" }
            client.killContainerCmd(it.containerId).withSignal("9").exec()

            logger.info { "removing container '${it.containerId}'" }
            client.removeContainerCmd(it.containerId).withForce(true).withRemoveVolumes(true).exec()
        }
    }

    override fun afterAll(context: ExtensionContext) {

        val client = DockerClientFactory.instance().client()

        logger.info { "removing network '${network.id}'" }
        network.close()
        client.removeNetworkCmd(network.id).exec()
    }

}