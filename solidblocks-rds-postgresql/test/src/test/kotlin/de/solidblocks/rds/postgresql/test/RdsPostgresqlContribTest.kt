package de.solidblocks.rds.postgresql.test

import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.output.Slf4jLogConsumer
import java.io.StringReader
import java.util.*
import javax.json.Json


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RdsTestBedExtension::class)
class RdsPostgresqlContribTest {

    private val logger = KotlinLogging.logger {}

    @Test
    fun testBackupInfoTakesArguments(testBed: RdsTestBed) {

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

        println(container.execInContainer("/rds/bin/backup-info.sh"))
        val backupInfoJsonRaw = container.execInContainer("/rds/bin/backup-info.sh", "--output=json").stdout

        val backupInfoJson = Json.createReader(StringReader(backupInfoJsonRaw)).readArray()
        assertThat(backupInfoJson[0]).isNotNull
    }

}
