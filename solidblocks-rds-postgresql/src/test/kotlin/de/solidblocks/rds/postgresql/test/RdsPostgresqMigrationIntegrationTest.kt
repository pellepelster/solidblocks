package de.solidblocks.rds.postgresql.test

import de.solidblocks.rds.postgresql.test.extensions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*


@ExtendWith(RdsTestBedExtension::class)
class RdsPostgresqMigrationIntegrationTest {

    companion object {
        val database = "database1"
    }

    @Test
    fun testMigrations(rdsTestBed: RdsTestBed) {

        val dataDir = initWorldReadableTempDir()
        val localBackupDir = initWorldReadableTempDir()

        val oldContainer = rdsTestBed.createAndStartPostgresContainer("ghcr.io/pellepelster/solidblocks-rds-postgresql:v0.1.17",
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
            ), dataDir
        ) {
            it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }

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

        oldContainer.createJdbi().also {
            it.waitForReady()
            it.createUserTable()
            it.insertUser(username)
            it.assertHasUserWithName(username)
        }


        oldContainer.stop()
        rdsTestBed.logConsumer.clear()

        val newContainer14 = rdsTestBed.createAndStartPostgresContainer(14,
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
            ), dataDir
        ) {
            it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }
        newContainer14.start()

        with(rdsTestBed.logConsumer) {
            // on second start with persistent storage no initializing ord backup should be executed
            waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
            assertHasLogLine("[solidblocks-rds-postgresql] old directory layout detected, migrating data files from '/storage/data/database1' to '/storage/data/database1/14'")
            assertHasLogLine("[solidblocks-rds-postgresql] data dir is not empty")
            assertHasNoLogLine("[solidblocks-rds-postgresql] initializing database instance")
            assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
            assertHasNoLogLine("[solidblocks-rds-postgresql] executing initial backup")
            waitForLogLine("database system is ready to accept connections")
        }
        newContainer14.execInContainer("backup-full.sh")
        newContainer14.createJdbi().also {
            it.waitForReady()
            it.assertHasUserWithName(username)
        }

        newContainer14.stop()
        rdsTestBed.logConsumer.clear()

        val newContainer15 = rdsTestBed.createAndStartPostgresContainer(15,
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
            ), dataDir
        ) {
            it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }
        newContainer15.start()

        with(rdsTestBed.logConsumer) {
            // on second start with persistent storage no initializing ord backup should be executed
            waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
            waitForLogLine("[solidblocks-rds-postgresql] found old version data in")
            waitForLogLine("database system is ready to accept connections")
        }
        newContainer15.createJdbi().also {
            it.waitForReady()
            it.assertHasUserWithName(username)
        }

        newContainer15.stop()
    }
}
