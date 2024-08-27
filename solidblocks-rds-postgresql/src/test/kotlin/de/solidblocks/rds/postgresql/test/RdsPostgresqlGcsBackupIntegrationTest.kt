package de.solidblocks.rds.postgresql.test

import de.solidblocks.rds.postgresql.test.extensions.GcsTestBed
import de.solidblocks.rds.postgresql.test.extensions.GcsTestBedExtension
import de.solidblocks.rds.postgresql.test.extensions.RdsTestBed
import de.solidblocks.rds.postgresql.test.extensions.RdsTestBedExtension
import de.solidblocks.rds.postgresql.test.extensions.assertHasUserWithName
import de.solidblocks.rds.postgresql.test.extensions.createJdbi
import de.solidblocks.rds.postgresql.test.extensions.createUserTable
import de.solidblocks.rds.postgresql.test.extensions.initWorldReadableTempDir
import de.solidblocks.rds.postgresql.test.extensions.insertUser
import de.solidblocks.rds.postgresql.test.extensions.waitForReady
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.shaded.org.bouncycastle.util.encoders.Base64
import java.io.File
import java.util.UUID

@ExtendWith(RdsTestBedExtension::class)
@ExtendWith(GcsTestBedExtension::class)
class RdsPostgresqlGcsBackupIntegrationTest {

    companion object {

        val s3BackupEnv = mapOf(
            "DB_BACKUP_GCS" to "1",
            "DB_BACKUP_GCS_SERVICE_KEY_BASE64" to System.getenv("DB_BACKUP_GCS_SERVICE_KEY_BASE64")
        )
    }

    @Test
    fun testDatabaseKeepsDataBetweenRestarts(rdsTestBed: RdsTestBed, gcsTestBed: GcsTestBed) {

        val dataDir = initWorldReadableTempDir()
        val container = rdsTestBed.createAndStartPostgresContainer(
            14,
            s3BackupEnv + mapOf("DB_BACKUP_GCS_BUCKET" to gcsTestBed.bucket), dataDir
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

}
