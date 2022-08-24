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
class RdsPostgresqlMinioBackupIntegrationTest {

    private val logger = KotlinLogging.logger {}

    companion object {
        val backupHost = "minio"
        val bucket = "database1-backup"
        val accessKey = "database1-user1"
        val secretKey = "ccbaa67e-cf26-432f-a11f-0c9e72abccf8"
        val database = "database1"
        val databaseUser = "user1"
        val databasePassword = "password1"

        val minioCertificatePrivateBase64 =
            Base64.getEncoder()
                .encodeToString(
                    RdsPostgresqlMinioBackupIntegrationTest::class.java.getResource("/minio.key.pem").readBytes()
                )
        val minioCertificatePublicBase64 =
            Base64.getEncoder()
                .encodeToString(
                    RdsPostgresqlMinioBackupIntegrationTest::class.java.getResource("/minio.pem").readBytes()
                )
        val caPublicBase64 =
            Base64.getEncoder()
                .encodeToString(RdsPostgresqlMinioBackupIntegrationTest::class.java.getResource("/ca.pem").readBytes())


        val s3BackupEnv = mapOf(
            "DB_BACKUP_S3" to "1",
            "DB_BACKUP_S3_CA_PUBLIC_KEY" to caPublicBase64,
            "DB_BACKUP_S3_HOST" to backupHost,
            "DB_BACKUP_S3_BUCKET" to bucket,
            "DB_BACKUP_S3_ACCESS_KEY" to accessKey,
            "DB_BACKUP_S3_SECRET_KEY" to secretKey
        )
    }


    @Test
    fun testDatabaseKeepsDataBetweenRestarts(testBed: RdsTestBed) {

        val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))

        testBed.createAndStartMinioContainer()

        val dataDir = initWorldReadableTempDir()
        val container = testBed.createAndStartPostgresContainer(
            s3BackupEnv, dataDir, logConsumer
        )

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
        container.start()

        // on second start with persistent storage no initializing ord backup should be executed
        logConsumer.waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] data dir is not empty")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] initializing database instance")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] executing initial backup")
        logConsumer.waitForLogLine("database system is ready to accept connections")

        container.createJdbi().also {

            it.waitForReady()

            assertThat(it.selectAllUsers()).filteredOn {
                it["name"] == user
            }.hasSize(1)
        }
    }

    @Test
    fun testRestoreDatabaseFromFullBackup(rdsTestBed: RdsTestBed) {


        val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))

        rdsTestBed.createAndStartMinioContainer()

        val postgresContainer1 = rdsTestBed.createAndStartPostgresContainer(
            s3BackupEnv, initWorldReadableTempDir(), logConsumer
        )

        // on first start instance should be initialized and an initial backup should be executed
        logConsumer.waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] initializing database instance")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] executing initial backup")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
        logConsumer.waitForLogLine("database system is ready to accept connections")


        val user = UUID.randomUUID().toString()

        postgresContainer1.createJdbi().also {

            it.waitForReady()
            it.createUserTable()
            it.insertUser(user)

            assertThat(it.selectAllUsers()).filteredOn {
                it["name"] == user
            }.hasSize(1)
        }


        postgresContainer1.execInContainer("/rds/bin/backup-full.sh")

        postgresContainer1.stop()
        logConsumer.clear()

        val postgresContainer2 = rdsTestBed.createAndStartPostgresContainer(
            s3BackupEnv, initWorldReadableTempDir(), logConsumer
        )


        // on second start without persistent storage restore should be executed
        logConsumer.waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] initializing database instance")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] restoring database from backup")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] executing initial backup")
        logConsumer.waitForLogLine("database system is ready to accept connections")

        postgresContainer2.createJdbi().also {

            it.waitForReady()

            assertThat(it.selectAllUsers()).filteredOn {
                it["name"] == user
            }.hasSize(1)
        }
    }

    @Test
    fun testRestoreDatabaseFromIncrementalBackup(rdsTestBed: RdsTestBed) {

        val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))

        rdsTestBed.createAndStartMinioContainer()

        val postgresContainer1 = rdsTestBed.createAndStartPostgresContainer(
            s3BackupEnv, initWorldReadableTempDir(), logConsumer
        )

        // on first start instance should be initialized and an initial backup should be executed
        logConsumer.waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] initializing database instance")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] executing initial backup")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
        logConsumer.waitForLogLine("database system is ready to accept connections")


        val user1 = UUID.randomUUID().toString()

        postgresContainer1.createJdbi().also {

            it.waitForReady()
            it.createUserTable()
            it.insertUser(user1)

            assertThat(it.selectAllUsers()).filteredOn {
                it["name"] == user1
            }.hasSize(1)
        }


        postgresContainer1.execInContainer("/rds/bin/backup-full.sh")

        val user2 = UUID.randomUUID().toString()
        postgresContainer1.createJdbi().also {
            it.insertUser(user2)
            assertThat(it.selectAllUsers()).filteredOn {
                it["name"] == user2
            }.hasSize(1)
        }


        postgresContainer1.execInContainer("/rds/bin/backup-incr.sh")

        postgresContainer1.stop()
        logConsumer.clear()

        val postgresContainer2 = rdsTestBed.createAndStartPostgresContainer(
            s3BackupEnv, initWorldReadableTempDir(), logConsumer
        )


        // on second start without persistent storage restore should be executed
        logConsumer.waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] initializing database instance")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] restoring database from backup")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] executing initial backup")
        logConsumer.waitForLogLine("database system is ready to accept connections")

        postgresContainer2.createJdbi().also {

            it.waitForReady()

            assertThat(it.selectAllUsers()).filteredOn {
                it["name"] == user1
            }.hasSize(1)
            assertThat(it.selectAllUsers()).filteredOn {
                it["name"] == user2
            }.hasSize(1)
        }
    }

    @Test
    fun testRestoreDatabaseFromDifferentialBackup(rdsTestBed: RdsTestBed) {


        val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))

        rdsTestBed.createAndStartMinioContainer()

        val postgresContainer1 = rdsTestBed.createAndStartPostgresContainer(
            s3BackupEnv, initWorldReadableTempDir(), logConsumer
        )

        // on first start instance should be initialized and an initial backup should be executed
        logConsumer.waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] initializing database instance")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] executing initial backup")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
        logConsumer.waitForLogLine("database system is ready to accept connections")


        val user1 = UUID.randomUUID().toString()

        postgresContainer1.createJdbi().also {

            it.waitForReady()
            it.createUserTable()
            it.insertUser(user1)

            assertThat(it.selectAllUsers()).filteredOn {
                it["name"] == user1
            }.hasSize(1)
        }


        postgresContainer1.execInContainer("/rds/bin/backup-full.sh")

        val user2 = UUID.randomUUID().toString()
        postgresContainer1.createJdbi().also {
            it.insertUser(user2)
            assertThat(it.selectAllUsers()).filteredOn {
                it["name"] == user2
            }.hasSize(1)
        }


        postgresContainer1.execInContainer("/rds/bin/backup-diff.sh")

        postgresContainer1.stop()
        logConsumer.clear()

        val postgresContainer2 = rdsTestBed.createAndStartPostgresContainer(
            s3BackupEnv, initWorldReadableTempDir(), logConsumer
        )


        // on second start without persistent storage restore should be executed
        logConsumer.waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] initializing database instance")
        logConsumer.assertHasLogLine("[solidblocks-rds-postgresql] restoring database from backup")
        logConsumer.assertHasNoLogLine("[solidblocks-rds-postgresql] executing initial backup")
        logConsumer.waitForLogLine("database system is ready to accept connections")

        postgresContainer2.createJdbi().also {

            it.waitForReady()

            assertThat(it.selectAllUsers()).filteredOn {
                it["name"] == user1
            }.hasSize(1)
            assertThat(it.selectAllUsers()).filteredOn {
                it["name"] == user2
            }.hasSize(1)
        }
    }
}
