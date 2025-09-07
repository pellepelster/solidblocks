package de.solidblocks.rds.postgresql.test

import de.solidblocks.rds.postgresql.test.extensions.*
import java.util.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(RdsTestBedExtension::class)
@ExtendWith(HetznerTestBedExtension::class)
@Disabled
class RdsPostgresqlHetznerS3BackupIntegrationTest {

  companion object {
    val s3BackupEnv =
        mapOf(
            "DB_BACKUP_S3" to "1",
            "DB_BACKUP_S3_HOST" to "fsn1.your-objectstorage.com",
            "DB_BACKUP_S3_ACCESS_KEY" to System.getenv("HETZNER_S3_ACCESS_KEY"),
            "DB_BACKUP_S3_SECRET_KEY" to System.getenv("HETZNER_S3_SECRET_KEY"),
        )
  }

  @Test
  fun testRestoreDatabaseFromFullBackup(rdsTestBed: RdsTestBed, hetznerTestBed: HetznerTestBed) {
    val postgresContainer1 =
        rdsTestBed.createAndStartPostgresContainer(
            17,
            s3BackupEnv + mapOf("DB_BACKUP_S3_BUCKET" to hetznerTestBed.bucketName),
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
            17,
            s3BackupEnv + mapOf("DB_BACKUP_S3_BUCKET" to hetznerTestBed.bucketName),
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
}
