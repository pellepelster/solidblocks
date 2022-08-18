package de.solidblocks.rds.postgresql.test

import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import java.util.*


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RdsTestBedExtension::class)
class RdsPostgresqlIntegrationTest {

    private val logger = KotlinLogging.logger {}

    companion object {
        val backupHost = "minio"
        val bucket = "database1-backup"
        val accessKey = "database1-user1"
        val secretKey = "ccbaa67e-cf26-432f-a11f-0c9e72abccf8"
        val database = "database1"
        val databaseUser = "user1"
        val databasePassword = "password1"

        val minioCertificatePrivate =
                RdsPostgresqlIntegrationTest::class.java.getResource("/minio.key.pem").readText()
        val minioCertificatePublic =
                RdsPostgresqlIntegrationTest::class.java.getResource("/minio.pem").readText()
        val caPublic =
                RdsPostgresqlIntegrationTest::class.java.getResource("/ca.pem").readText()

    }

    @AfterAll
    fun cleanupAll() {
        val client = DockerClientFactory.instance().client()

        //logger.info { "removing network '${network.id}'" }
        //client.removeNetworkCmd(network.id).exec()
    }

    @Test
    fun testDatabaseKeepsDataBetweenRestarts(testBed: RdsTestBed) {

        val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))

        testBed.createAndStartMinioContainer()

        val dataDir = initWorldReadableTempDir()
        val container = testBed.createAndStartPostgresContainer(dataDir, logConsumer)

        // on first start instance should be initialized and an initial backup should be executed
        logConsumer.waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] initializing database instance")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] executing initial backup")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
        logConsumer.waitForLogLine("database system is ready to accept connections")


        val user = UUID.randomUUID().toString()

        container.createJdbi().also {
            it.useHandle<RuntimeException> {
                it.execute("CREATE TABLE \"user\" (id VARCHAR PRIMARY KEY, \"name\" VARCHAR)")
            }

            it.useHandle<RuntimeException> {
                it.createUpdate("INSERT INTO \"user\" (id, \"name\") VALUES (?, ?)")
                        .bind(0, UUID.randomUUID())
                        .bind(1, user)
                        .execute()
            }

            val result = it.withHandle<List<Map<String, Any>>, RuntimeException> {
                it.createQuery("SELECT * FROM \"user\" ORDER BY \"name\"")
                        .mapToMap()
                        .list()
            }
            assertThat(result).filteredOn {
                it["name"] == user
            }.hasSize(1)
        }


        container.stop()
        logConsumer.clear()
        container.start()

        // on second start with persistent storage no initializing ord backup should be executed
        logConsumer.waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] data dir is not empty")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] initializing database instance")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] executing initial backup")
        logConsumer.waitForLogLine("database system is ready to accept connections")

        container.createJdbi().also {
            val result = it.withHandle<List<Map<String, Any>>, RuntimeException> {
                it.createQuery("SELECT * FROM \"user\" ORDER BY \"name\"")
                        .mapToMap()
                        .list()
            }
            assertThat(result).filteredOn {
                it["name"] == user
            }.hasSize(1)
        }
    }

    @Test
    fun testRestoreDatabaseFromFullBackup(rdsTestBed: RdsTestBed) {


        val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))

        rdsTestBed.createAndStartMinioContainer()

        val postgresContainer1 = rdsTestBed.createAndStartPostgresContainer(initWorldReadableTempDir(), logConsumer)

        // on first start instance should be initialized and an initial backup should be executed
        logConsumer.waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] initializing database instance")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] executing initial backup")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
        logConsumer.waitForLogLine("database system is ready to accept connections")


        val user = UUID.randomUUID().toString()

        postgresContainer1.createJdbi().also {
            it.useHandle<RuntimeException> {
                it.execute("CREATE TABLE \"user\" (id VARCHAR PRIMARY KEY, \"name\" VARCHAR)")
            }

            it.useHandle<RuntimeException> {
                it.createUpdate("INSERT INTO \"user\" (id, \"name\") VALUES (?, ?)")
                        .bind(0, UUID.randomUUID())
                        .bind(1, user)
                        .execute()
            }

            val result = it.withHandle<List<Map<String, Any>>, RuntimeException> {
                it.createQuery("SELECT * FROM \"user\" ORDER BY \"name\"")
                        .mapToMap()
                        .list()
            }
            assertThat(result).filteredOn {
                it["name"] == user
            }.hasSize(1)
        }


        postgresContainer1.execInContainer("/rds/bin/backup.sh")

        postgresContainer1.stop()
        logConsumer.clear()

        val postgresContainer2 = rdsTestBed.createAndStartPostgresContainer(initWorldReadableTempDir(), logConsumer)


        // on second start without persistent storage restore should be executed
        logConsumer.waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] initializing database instance")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] restoring database from backup")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] executing initial backup")
        logConsumer.waitForLogLine("database system is ready to accept connections")

        postgresContainer2.createJdbi().also {
            val result = it.withHandle<List<Map<String, Any>>, RuntimeException> {
                it.createQuery("SELECT * FROM \"user\" ORDER BY \"name\"")
                        .mapToMap()
                        .list()
            }
            assertThat(result).filteredOn {
                it["name"] == user
            }.hasSize(1)
        }
    }

    @Test
    fun doesNotStartIfNoStorageIsMounted() {

        val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))

        val container = GenericContainer("solidblocks-rds-postgresql").apply {
            withLogConsumer(logConsumer)
            withEnv(
                    mapOf(
                            "DB_BACKUP_S3_HOST" to backupHost,
                            "DB_BACKUP_S3_BUCKET" to bucket,
                            "DB_BACKUP_S3_ACCESS_KEY" to accessKey,
                            "DB_BACKUP_S3_SECRET_KEY" to secretKey,

                            "DB_DATABASE" to database,
                            "DB_USERNAME" to databaseUser,
                            "DB_PASSWORD" to databasePassword,

                            "CA_PUBLIC_KEY" to caPublic,
                    )
            )
        }

        Assertions.assertThrows(ContainerLaunchException::class.java) {
            container.start()
        }

        logConsumer.waitForLogLine("[solidblocks-rds-postgresql] storage dir '/storage/local' not mounted")
    }

}
