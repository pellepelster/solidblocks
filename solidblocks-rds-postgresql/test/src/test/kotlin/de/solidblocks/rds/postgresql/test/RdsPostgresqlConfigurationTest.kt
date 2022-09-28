package de.solidblocks.rds.postgresql.test

import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.output.Slf4jLogConsumer
import java.util.*


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RdsTestBedExtension::class)
class RdsPostgresqlConfigurationTest {

    private val logger = KotlinLogging.logger {}

    @Test
    fun testChangesUsernameAndPasswordAfterRestart(testBed: RdsTestBed) {

        val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))

        val dataDir = initWorldReadableTempDir()
        val localBackupDir = initWorldReadableTempDir()

        val container = testBed.createAndStartPostgresContainer(
                mapOf(
                        "DB_BACKUP_LOCAL" to "1",
                ), dataDir, logConsumer
        ) {
            it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }

        // on first start instance should be initialized and an initial backup should be executed
        logConsumer.waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] initializing database instance")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] executing initial backup")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
        logConsumer.waitForLogLine("database system is ready to accept connections")


        val user = UUID.randomUUID().toString()

        container.createJdbi().also {

            it.waitForReady()
            it.createUserTable()
            it.insertUser(user)

            assertThat(it.selectAllUsers()).filteredOn {
                it["name"] == user
            }.hasSize(1)
        }


        container.stop()
        logConsumer.clear()
        container.withEnv(mapOf(
                "DB_USERNAME_${RdsPostgresqlMinioBackupIntegrationTest.database}" to "new-user",
                "DB_PASSWORD_${RdsPostgresqlMinioBackupIntegrationTest.database}" to "new-password",
        )).start()

        // on second start with persistent storage no initializing ord backup should be executed
        logConsumer.waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] data dir is not empty")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] initializing database instance")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] executing initial backup")
        logConsumer.waitForLogLine("database system is ready to accept connections")

        container.createJdbi("new-user", "new-password").also {

            it.waitForReady()

            assertThat(it.selectAllUsers()).filteredOn {
                it["name"] == user
            }.hasSize(1)
        }
    }

    @Test
    fun testChangesUsernameAndPasswordAfterRestore(rdsTestBed: RdsTestBed) {

        val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))

        val localBackupDir = initWorldReadableTempDir()
        val container1 = rdsTestBed.createAndStartPostgresContainer(
                mapOf(
                        "DB_BACKUP_LOCAL" to "1",
                ), initWorldReadableTempDir(), logConsumer
        ) {
            it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }

        // on first start instance should be initialized and an initial backup should be executed
        logConsumer.waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] initializing database instance")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] executing initial backup")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
        logConsumer.waitForLogLine("database system is ready to accept connections")


        val user = UUID.randomUUID().toString()

        container1.createJdbi().also {

            it.waitForReady()
            it.createUserTable()
            it.insertUser(user)

            assertThat(it.selectAllUsers()).filteredOn {
                it["name"] == user
            }.hasSize(1)
        }


        container1.execInContainer("/rds/bin/backup-full.sh")

        container1.stop()
        logConsumer.clear()

        val container2 = rdsTestBed.createAndStartPostgresContainer(
                mapOf(
                        "DB_BACKUP_LOCAL" to "1",
                        "DB_USERNAME_${RdsPostgresqlMinioBackupIntegrationTest.database}" to "new-user",
                        "DB_PASSWORD_${RdsPostgresqlMinioBackupIntegrationTest.database}" to "new-password",
                        ), initWorldReadableTempDir(), logConsumer
        ) {
            it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }


        // on second start without persistent storage restore should be executed
        logConsumer.waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] initializing database instance")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] restoring database from backup")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] executing initial backup")
        logConsumer.waitForLogLine("database system is ready to accept connections")

        container2.createJdbi("new-user", "new-password").also {

            it.waitForReady()

            assertThat(it.selectAllUsers()).filteredOn {
                it["name"] == user
            }.hasSize(1)
        }

    }

    @Test
    fun testSetCustomPostgresConfig(testBed: RdsTestBed) {

        val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))

        val dataDir = initWorldReadableTempDir()
        val localBackupDir = initWorldReadableTempDir()

        val container = testBed.createAndStartPostgresContainer(
                mapOf(
                        "DB_BACKUP_LOCAL" to "1",
                        "DB_POSTGRES_EXTRA_CONFIG" to "checkpoint_timeout = 301",
                ), dataDir, logConsumer
        ) {
            it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }

        // on first start instance should be initialized and an initial backup should be executed
        logConsumer.waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] initializing database instance")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] executing initial backup")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
        logConsumer.waitForLogLine("database system is ready to accept connections")


        val settings = container.createJdbi().withHandle<List<Map<String, Any>>, RuntimeException> {
            it.createQuery("SELECT * FROM \"pg_settings\" ORDER BY \"name\"")
                    .mapToMap()
                    .list()

        }

        //settings.forEach {
        //    println("${it["name"]}=${it["setting"]}")
        //}

        assertThat(settings.filter { it["name"] == "checkpoint_timeout" }.first()["setting"]).isEqualTo("301")
    }

    @Test
    fun testHasCreateSchemaPermissions(testBed: RdsTestBed) {

        val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))

        val dataDir = initWorldReadableTempDir()
        val localBackupDir = initWorldReadableTempDir()

        val container = testBed.createAndStartPostgresContainer(
                mapOf(
                        "DB_BACKUP_LOCAL" to "1"
                ), dataDir, logConsumer
        ) {
            it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }

        logConsumer.waitForLogLine("database system is ready to accept connections")

        container.createJdbi().useHandle<Exception> {
            it.execute("CREATE SCHEMA myschema;")
        }
    }

    @Test
    fun testHasAdminPassword(testBed: RdsTestBed) {

        val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))

        val dataDir = initWorldReadableTempDir()
        val localBackupDir = initWorldReadableTempDir()

        val container1 = testBed.createAndStartPostgresContainer(
                mapOf(
                        "DB_ADMIN_PASSWORD" to "my-database-password",
                        "DB_BACKUP_LOCAL" to "1"
                ), dataDir, logConsumer
        ) {
            it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }

        logConsumer.waitForLogLine("database system is ready to accept connections")

        container1.createJdbi("rds", "my-database-password").useHandle<Exception> {
            it.execute("SELECT version()")
        }

        container1.stop()
        logConsumer.clear()

        val container2 = testBed.createAndStartPostgresContainer(
                mapOf(
                        "DB_ADMIN_PASSWORD" to "new-database-password",
                        "DB_BACKUP_LOCAL" to "1"
                ), dataDir, logConsumer
        ) {
            it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }

        logConsumer.waitForLogLine("database system is ready to accept connections")

        container2.createJdbi("rds", "new-database-password").useHandle<Exception> {
            it.execute("SELECT version()")
        }

    }

    @Test
    fun testCreatesExtraDatabases(testBed: RdsTestBed) {

        val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))

        val dataDir = initWorldReadableTempDir()
        val localBackupDir = initWorldReadableTempDir()

        val container = testBed.createAndStartPostgresContainer(
                mapOf(
                        "DB_BACKUP_LOCAL" to "1",
                        "DB_DATABASE_extra_database_1" to "extra-database-1",
                        "DB_USERNAME_extra_database_1" to "extra-user-1",
                        "DB_PASSWORD_extra_database_1" to "extra-password-1",
                ), dataDir, logConsumer
        ) {
            it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }

        // on first start instance should be initialized and an initial backup should be executed
        logConsumer.waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] initializing database instance")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] executing initial backup")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
        logConsumer.waitForLogLine("database system is ready to accept connections")

        container.createJdbi("extra-user-1", "extra-password-1", "extra-database-1").also {
            it.waitForReady()
        }
    }
}
