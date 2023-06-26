package de.solidblocks.rds.postgresql.test

import de.solidblocks.rds.postgresql.test.extensions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(RdsTestBedExtension::class)
@ExtendWith(AwsTestBedExtension::class)
class RdsPostgresqlAwsS3BackupIntegrationTest {

    companion object {

        val s3BackupEnv = mapOf(
            "DB_BACKUP_S3" to "1",
            "DB_BACKUP_S3_ACCESS_KEY" to System.getenv("AWS_ACCESS_KEY_ID"),
            "DB_BACKUP_S3_SECRET_KEY" to System.getenv("AWS_SECRET_ACCESS_KEY")
        )
    }

    @Test
    fun testDatabaseKeepsDataBetweenRestarts(rdsTestBed: RdsTestBed, awsTestBed: AwsTestBed) {

        val dataDir = initWorldReadableTempDir()
        val container = rdsTestBed.createAndStartPostgresContainer(
            s3BackupEnv + mapOf("DB_BACKUP_S3_BUCKET" to awsTestBed.bucket), dataDir
        )

        // on first start instance should be initialized and an initial backup should be executed
        with(rdsTestBed.logConsumer) {
            waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
            assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
            assertHasLogLine("[solidblocks-rds-postgresql] initializing database instance")
            assertHasLogLine("[solidblocks-rds-postgresql] executing initial backup")
            assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
            waitForLogLine("database system is ready to accept connections")
        }

        val username = UUID.randomUUID().toString()

        container.createJdbi().also {
            it.waitForReady()
            it.createUserTable()
            it.insertUser(username)
            it.assertHasUserWithName(username)
        }

        container.stop()
        rdsTestBed.logConsumer.clear()
        container.start()

        with(rdsTestBed.logConsumer) {
            // on second start with persistent storage no initializing ord backup should be executed
            waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
            assertHasLogLine("[solidblocks-rds-postgresql] data dir is not empty")
            assertHasNoLogLine("[solidblocks-rds-postgresql] initializing database instance")
            assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
            assertHasNoLogLine("[solidblocks-rds-postgresql] executing initial backup")
            waitForLogLine("database system is ready to accept connections")
        }

        container.createJdbi().also {
            it.waitForReady()
            it.assertHasUserWithName(username)
        }
    }

    @Test
    fun testRestoreDatabaseFromFullBackup(rdsTestBed: RdsTestBed, awsTestBed: AwsTestBed) {
        val postgresContainer1 = rdsTestBed.createAndStartPostgresContainer(
            s3BackupEnv + mapOf("DB_BACKUP_S3_BUCKET" to awsTestBed.bucket), initWorldReadableTempDir()
        )

        with(rdsTestBed.logConsumer) {
            // on first start instance should be initialized and an initial backup should be executed
            waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
            assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
            assertHasLogLine("[solidblocks-rds-postgresql] initializing database instance")
            assertHasLogLine("[solidblocks-rds-postgresql] executing initial backup")
            assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
            waitForLogLine("database system is ready to accept connections")
        }


        val username = UUID.randomUUID().toString()

        postgresContainer1.createJdbi().also {

            it.waitForReady()
            it.createUserTable()
            it.insertUser(username)
            it.assertHasUserWithName(username)
        }


        postgresContainer1.execInContainer("/rds/bin/backup-full.sh")

        postgresContainer1.stop()
        rdsTestBed.logConsumer.clear()

        val postgresContainer2 = rdsTestBed.createAndStartPostgresContainer(
            s3BackupEnv + mapOf("DB_BACKUP_S3_BUCKET" to awsTestBed.bucket), initWorldReadableTempDir()
        )


        with(rdsTestBed.logConsumer) {
            // on second start without persistent storage restore should be executed
            waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
            assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
            assertHasNoLogLine("[solidblocks-rds-postgresql] initializing database instance")
            assertHasLogLine("[solidblocks-rds-postgresql] restoring database from backup")
            assertHasNoLogLine("[solidblocks-rds-postgresql] executing initial backup")
            waitForLogLine("database system is ready to accept connections")
        }

        postgresContainer2.createJdbi().also {
            it.waitForReady()
            it.assertHasUserWithName(username)
        }
    }

    @Test
    fun testRestoreDatabaseFromIncrementalBackup(rdsTestBed: RdsTestBed, awsTestBed: AwsTestBed) {

        val postgresContainer1 = rdsTestBed.createAndStartPostgresContainer(
            s3BackupEnv + mapOf("DB_BACKUP_S3_BUCKET" to awsTestBed.bucket), initWorldReadableTempDir()
        )

        with(rdsTestBed.logConsumer) {
            // on first start instance should be initialized and an initial backup should be executed
            waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
            assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
            assertHasLogLine("[solidblocks-rds-postgresql] initializing database instance")
            assertHasLogLine("[solidblocks-rds-postgresql] executing initial backup")
            assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
            waitForLogLine("database system is ready to accept connections")
        }

        val username1 = UUID.randomUUID().toString()

        postgresContainer1.createJdbi().also {

            it.waitForReady()
            it.createUserTable()
            it.insertUser(username1)
            it.assertHasUserWithName(username1)
        }


        postgresContainer1.execInContainer("/rds/bin/backup-full.sh")

        val username2 = UUID.randomUUID().toString()
        postgresContainer1.createJdbi().also {
            it.insertUser(username2)
            it.assertHasUserWithName(username2)
        }

        postgresContainer1.execInContainer("/rds/bin/backup-incr.sh")
        postgresContainer1.stop()
        rdsTestBed.logConsumer.clear()

        val postgresContainer2 = rdsTestBed.createAndStartPostgresContainer(
            s3BackupEnv + mapOf("DB_BACKUP_S3_BUCKET" to awsTestBed.bucket), initWorldReadableTempDir()
        )


        with(rdsTestBed.logConsumer) {
            // on second start without persistent storage restore should be executed
            waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
            assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
            assertHasNoLogLine("[solidblocks-rds-postgresql] initializing database instance")
            assertHasLogLine("[solidblocks-rds-postgresql] restoring database from backup")
            assertHasNoLogLine("[solidblocks-rds-postgresql] executing initial backup")
            waitForLogLine("database system is ready to accept connections")
        }


        postgresContainer2.createJdbi().also {
            it.waitForReady()
            it.assertHasUserWithName(username1)
            it.assertHasUserWithName(username2)
        }
    }

    @Test
    fun testRestoreDatabaseFromDifferentialBackup(rdsTestBed: RdsTestBed, awsTestBed: AwsTestBed) {

        val postgresContainer1 = rdsTestBed.createAndStartPostgresContainer(
            s3BackupEnv + mapOf("DB_BACKUP_S3_BUCKET" to awsTestBed.bucket), initWorldReadableTempDir()
        )

        with(rdsTestBed.logConsumer) {
            waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
            assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
            assertHasLogLine("[solidblocks-rds-postgresql] initializing database instance")
            assertHasLogLine("[solidblocks-rds-postgresql] executing initial backup")
            assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
            waitForLogLine("database system is ready to accept connections")
        }


        val username1 = UUID.randomUUID().toString()

        postgresContainer1.createJdbi().also {
            it.waitForReady()
            it.createUserTable()
            it.insertUser(username1)
            it.assertHasUserWithName(username1)
        }


        postgresContainer1.execInContainer("/rds/bin/backup-full.sh")

        val username2 = UUID.randomUUID().toString()
        postgresContainer1.createJdbi().also {
            it.insertUser(username2)
            it.assertHasUserWithName(username2)
        }


        postgresContainer1.execInContainer("/rds/bin/backup-diff.sh")

        postgresContainer1.stop()
        rdsTestBed.logConsumer.clear()

        val postgresContainer2 = rdsTestBed.createAndStartPostgresContainer(
            s3BackupEnv + mapOf("DB_BACKUP_S3_BUCKET" to awsTestBed.bucket), initWorldReadableTempDir()
        )

        with(rdsTestBed.logConsumer) {
            // on second start without persistent storage restore should be executed
            waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
            assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
            assertHasNoLogLine("[solidblocks-rds-postgresql] initializing database instance")
            assertHasLogLine("[solidblocks-rds-postgresql] restoring database from backup")
            assertHasNoLogLine("[solidblocks-rds-postgresql] executing initial backup")
            waitForLogLine("database system is ready to accept connections")
        }

        postgresContainer2.createJdbi().also {
            it.waitForReady()
            it.assertHasUserWithName(username1)
            it.assertHasUserWithName(username2)
        }
    }
}
