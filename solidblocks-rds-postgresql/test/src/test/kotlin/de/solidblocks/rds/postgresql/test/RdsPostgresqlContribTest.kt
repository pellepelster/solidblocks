package de.solidblocks.rds.postgresql.test

import de.solidblocks.rds.postgresql.test.extensions.RdsTestBed
import de.solidblocks.rds.postgresql.test.extensions.RdsTestBedExtension
import de.solidblocks.rds.postgresql.test.extensions.initWorldReadableTempDir
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.StringReader
import java.util.*
import javax.json.Json


@ExtendWith(RdsTestBedExtension::class)
class RdsPostgresqlContribTest {

    @Test
    fun testBackupInfoTakesArguments(rdsTestBed: RdsTestBed) {
        val dataDir = initWorldReadableTempDir()
        val localBackupDir = initWorldReadableTempDir()

        val container = rdsTestBed.createAndStartPostgresContainer(14,
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

        println(container.execInContainer("backup-info.sh"))
        val backupInfoJsonRaw = container.execInContainer("backup-info.sh", "--output=json").stdout

        val backupInfoJson = Json.createReader(StringReader(backupInfoJsonRaw)).readArray()
        assertThat(backupInfoJson[0]).isNotNull
    }

}
