package de.solidblocks.rds.postgresql.test

import de.solidblocks.rds.postgresql.test.extensions.*
import java.util.UUID
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@ExtendWith(RdsTestBedExtension::class)
@ExtendWith(AwsTestBedExtension::class)
@DisabledIfEnvironmentVariable(named = "SKIP_TESTS", matches = ".*integration.*")
class RdsPostgresqlAwsS3BackupIntegrationTest {

  companion object {

    val s3BackupEnv =
        mapOf(
            "DB_BACKUP_S3" to "1",
            "DB_BACKUP_S3_ACCESS_KEY" to System.getenv("AWS_ACCESS_KEY_ID"),
            "DB_BACKUP_S3_SECRET_KEY" to System.getenv("AWS_SECRET_ACCESS_KEY"),
        )
  }

  @ParameterizedTest
  @ValueSource(ints = [14, 15])
  fun testDatabaseKeepsDataBetweenRestarts(
      version: Int,
      rdsTestBed: RdsTestBed,
      awsTestBed: AwsTestBed,
  ) {
    val dataDir = initWorldReadableTempDir()

    val container =
        rdsTestBed.createAndStartPostgresContainer(
            version,
            s3BackupEnv + mapOf("DB_BACKUP_S3_BUCKET" to awsTestBed.bucket),
            dataDir,
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

    container.stop()
  }

  @Test
  fun testRestoreDatabaseFromFullBackup(rdsTestBed: RdsTestBed, awsTestBed: AwsTestBed) {
    val postgresContainer1 =
        rdsTestBed.createAndStartPostgresContainer(
            14,
            s3BackupEnv + mapOf("DB_BACKUP_S3_BUCKET" to awsTestBed.bucket),
            initWorldReadableTempDir(),
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

    postgresContainer1.execInContainer("backup-full.sh")

    postgresContainer1.stop()
    rdsTestBed.logConsumer.clear()

    val postgresContainer2 =
        rdsTestBed.createAndStartPostgresContainer(
            14,
            s3BackupEnv + mapOf("DB_BACKUP_S3_BUCKET" to awsTestBed.bucket),
            initWorldReadableTempDir(),
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

    postgresContainer2.stop()
  }

  @Test
  fun testRestoreOnlyFromFullBackup(rdsTestBed: RdsTestBed, awsTestBed: AwsTestBed) {
    val postgresContainer1 =
        rdsTestBed.createAndStartPostgresContainer(
            14,
            s3BackupEnv + mapOf("DB_BACKUP_S3_BUCKET" to awsTestBed.bucket),
            initWorldReadableTempDir(),
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

    postgresContainer1.execInContainer("backup-full.sh")

    postgresContainer1.stop()
    rdsTestBed.logConsumer.clear()

    val postgresContainer2 =
        rdsTestBed.createAndStartPostgresContainer(
            14,
            s3BackupEnv + mapOf("DB_BACKUP_S3_BUCKET" to awsTestBed.bucket),
            initWorldReadableTempDir(),
        ) {
          it.withCommand("restore-only")
        }

    with(rdsTestBed.logConsumer) {
      // on second start without persistent storage restore should be executed
      waitForLogLine("[solidblocks-rds-postgresql] starting db in restore-only mode")
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

    postgresContainer2.stop()
  }

  @Test
  fun testRestoreDatabaseFromIncrementalBackup(rdsTestBed: RdsTestBed, awsTestBed: AwsTestBed) {
    val postgresContainer1 =
        rdsTestBed.createAndStartPostgresContainer(
            14,
            s3BackupEnv + mapOf("DB_BACKUP_S3_BUCKET" to awsTestBed.bucket),
            initWorldReadableTempDir(),
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

    postgresContainer1.execInContainer("backup-full.sh")

    val username2 = UUID.randomUUID().toString()
    postgresContainer1.createJdbi().also {
      it.insertUser(username2)
      it.assertHasUserWithName(username2)
    }

    postgresContainer1.execInContainer("backup-incr.sh")
    postgresContainer1.stop()
    rdsTestBed.logConsumer.clear()

    val postgresContainer2 =
        rdsTestBed.createAndStartPostgresContainer(
            14,
            s3BackupEnv + mapOf("DB_BACKUP_S3_BUCKET" to awsTestBed.bucket),
            initWorldReadableTempDir(),
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

    postgresContainer2.stop()
  }

  @Test
  fun testRestoreDatabaseFromDifferentialBackup(rdsTestBed: RdsTestBed, awsTestBed: AwsTestBed) {
    val postgresContainer1 =
        rdsTestBed.createAndStartPostgresContainer(
            14,
            s3BackupEnv + mapOf("DB_BACKUP_S3_BUCKET" to awsTestBed.bucket),
            initWorldReadableTempDir(),
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

    logger.info { "[test] triggering full backup" }
    postgresContainer1.execInContainer("backup-full.sh")

    val username2 = UUID.randomUUID().toString()
    postgresContainer1.createJdbi().also {
      it.insertUser(username2)
      it.assertHasUserWithName(username2)
    }

    logger.info { "[test] triggering diff backup" }
    postgresContainer1.execInContainer("backup-diff.sh")

    postgresContainer1.stop()
    rdsTestBed.logConsumer.clear()

    val postgresContainer2 =
        rdsTestBed.createAndStartPostgresContainer(
            14,
            s3BackupEnv + mapOf("DB_BACKUP_S3_BUCKET" to awsTestBed.bucket),
            initWorldReadableTempDir(),
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

    postgresContainer2.stop()
  }
}
