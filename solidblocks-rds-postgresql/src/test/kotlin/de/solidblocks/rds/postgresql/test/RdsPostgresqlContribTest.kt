package de.solidblocks.rds.postgresql.test

import de.solidblocks.rds.postgresql.test.extensions.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.StringReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import javax.json.Json


@ExtendWith(RdsTestBedExtension::class)
class RdsPostgresqlContribTest {

    @Test
    fun testBackupInfoTakesArguments(rdsTestBed: RdsTestBed) {
        val dataDir = initWorldReadableTempDir()
        val localBackupDir = initWorldReadableTempDir()

        val container = rdsTestBed.createAndStartPostgresContainer(
            14,
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
        container.stop()
    }

    fun localIpAddresses() = sequence {
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        yield(address.getHostAddress())
                    }
                }
            }
        } catch (ex: SocketException) {
            ex.printStackTrace()
        }
    }

    @Test
    fun testLogsInvalidAccess(rdsTestBed: RdsTestBed) {
        val dataDir = initWorldReadableTempDir()
        val localBackupDir = initWorldReadableTempDir()

        val container = rdsTestBed.createAndStartPostgresContainer(
            14,
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

        val regexes = localIpAddresses().map {
            "${it.replace(".", "\\.")}.*\\s*FATAL:\\s*password authentication failed for user \"invalid\"\\s*".toRegex(RegexOption.MULTILINE)
        }

        try {
            container.createJdbi("invalid", "invalid", "invalid").also {
                it.createUserTable()
            }
        } catch (e: Exception) {
        }

        with(rdsTestBed.logConsumer) {
            waitForAnyLogLine(
                regexes.toList()
            )
        }
    }

}
