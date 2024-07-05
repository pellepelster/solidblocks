package de.solidblocks.rds.postgresql.test.extensions

import de.solidblocks.rds.postgresql.test.RdsPostgresqlMinioBackupIntegrationTest
import de.solidblocks.rds.postgresql.test.RdsPostgresqlMinioBackupIntegrationTest.Companion.database
import de.solidblocks.rds.postgresql.test.RdsPostgresqlMinioBackupIntegrationTest.Companion.databasePassword
import de.solidblocks.rds.postgresql.test.RdsPostgresqlMinioBackupIntegrationTest.Companion.databaseUser
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.images.PullPolicy
import java.io.File
import java.time.Duration

class RdsTestBed {

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

        logger.info { "[test] starting minio instance with storage dir '${storageDir}'" }
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
            it.withStartupTimeout(Duration.ofMinutes(6))
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
            logger.info { "[test] starting postgres container" }
            containers.add(it)
            it.start()
        }
    }

    fun clean() {
        val client = DockerClientFactory.instance().client()

        containers.mapNotNull { it.containerId }.forEach {
            try {
                logger.info { "[test] killing container '${it}'" }
                client.killContainerCmd(it).withSignal("9").exec()

                logger.info { "[test] removing container '${it}'" }
                client.removeContainerCmd(it).withForce(true).withRemoveVolumes(true).exec()
            } catch (e: Exception) {
                logger.info { "failed to kill container $it" }
            }
        }

        try {
            logger.info { "[test] removing network '${network.id}'" }
            network.close()
            client.removeNetworkCmd(network.id).exec()
        } catch (e: Exception) {
            logger.info { "failed to remove network ${network.id}" }
        }
    }
}