package de.solidblocks.cloudinit

import de.solidblocks.cloudinit.PostgresqlUserData
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import io.kotest.matchers.shouldBe
import org.awaitility.Awaitility
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.junit.jupiter.api.extension.ExtendWith
import java.sql.DriverManager
import java.time.Duration
import java.util.concurrent.TimeUnit

@ExtendWith(SolidblocksTest::class)
class PostgresqlUserDataTest {

    @Test
    @DisabledIfEnvironmentVariable(named = "SKIP_TESTS", matches = ".*integration.*")
    fun testLocalBackupAndRestore(testContext: SolidblocksTestContext) {
        val hetzner = testContext.hetzner(System.getenv("HCLOUD_TOKEN").toString())

        val dataVolume1 = hetzner.createVolume("${hetzner.testId}-data1")
        val backupVolume = hetzner.createVolume("${hetzner.testId}-backup")
        val sshKey = hetzner.createSSHKey()

        val backupConfiguration = BackupConfiguration("very-secret", LocalBackupTarget(backupVolume.linuxDevice))
        val userData =
            PostgresqlUserData(
                "instance1",
                "superuser-very-secret",
                dataVolume1.linuxDevice,
                backupConfiguration,
            )

        val server =
            hetzner.createServer(
                userData.render(),
                sshKey,
                volumes = listOf(backupVolume.id, dataVolume1.id),
            )
        server.waitForSuccessfulProvisioning()

        Awaitility.await().atMost(5, TimeUnit.MINUTES).pollInterval(Duration.ofSeconds(5)).until {
            server.host().portIsOpen(5432)
        }

        /**
         * insert some data
         */
        val jdbcUrl = "jdbc:postgresql://${server.host}:5432/postgres"
        createDatabaseAndInsertDog(jdbcUrl, "superuser-very-secret")

        /**
         *  trigger full backup
         */
        val ssh = server.ssh()
        ssh.command("systemctl start instance1-backup-full.service").exitCode shouldBe 0

        /**
         * delete server and data disk
         */
        hetzner.destroyServer(server)
        hetzner.destroyVolume(dataVolume1) shouldBe true

        /**
         * re-create server with new data disk
         */
        val dataVolume2 = hetzner.createVolume("${hetzner.testId}-data2")
        val recreatedUserData =
            PostgresqlUserData(
                "instance1",
                "superuser-very-secret",
                dataVolume2.linuxDevice,
                backupConfiguration,
            )

        val recreatedServer =
            hetzner.createServer(
                recreatedUserData.render(),
                sshKey,
                volumes = listOf(backupVolume.id, dataVolume2.id),
            )
        recreatedServer.waitForSuccessfulProvisioning()

        Awaitility.await().atMost(5, TimeUnit.MINUTES).pollInterval(Duration.ofSeconds(5)).until {
            recreatedServer.host().portIsOpen(5432)
        }

        val recreatedJdbcUrl = "jdbc:postgresql://${recreatedServer.host}:5432/postgres"
        getAllDogNames(recreatedJdbcUrl, "superuser-very-secret")[0] shouldBe "snoopy"
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "SKIP_TESTS", matches = ".*integration.*")
    fun testS3BackupAndRestore(testContext: SolidblocksTestContext) {
        val hetzner = testContext.hetzner(System.getenv("HCLOUD_TOKEN").toString())

        val dataVolume1 = hetzner.createVolume("${hetzner.testId}-data1")
        val sshKey = hetzner.createSSHKey()

        val bucket = testContext.aws().createBucket()

        val backupConfiguration = BackupConfiguration(
            "very-secret",
            S3BackupTarget(
                bucket,
                System.getenv("AWS_ACCESS_KEY_ID"),
                System.getenv("AWS_SECRET_ACCESS_KEY"),
            ),
        )
        val userData =
            PostgresqlUserData(
                "instance1",
                "superuser-very-secret",
                dataVolume1.linuxDevice,
                backupConfiguration,
            )

        val server =
            hetzner.createServer(
                userData.render(),
                sshKey,
                volumes = listOf(dataVolume1.id),
            )
        server.waitForSuccessfulProvisioning()

        Awaitility.await().atMost(5, TimeUnit.MINUTES).pollInterval(Duration.ofSeconds(5)).until {
            server.host().portIsOpen(5432)
        }

        /**
         * insert some data
         */
        val jdbcUrl = "jdbc:postgresql://${server.host}:5432/postgres"
        createDatabaseAndInsertDog(jdbcUrl, "superuser-very-secret")

        /**
         *  trigger full backup
         */
        val ssh = server.ssh()
        ssh.command("systemctl start instance1-backup-full.service").exitCode shouldBe 0

        /**
         * delete server and data disk
         */
        hetzner.destroyServer(server)
        hetzner.destroyVolume(dataVolume1) shouldBe true

        /**
         * re-create server with new data disk
         */
        val dataVolume2 = hetzner.createVolume("${hetzner.testId}-data2")
        val recreatedUserData =
            PostgresqlUserData(
                "instance1",
                "superuser-very-secret",
                dataVolume2.linuxDevice,
                backupConfiguration,
            )

        val recreatedServer =
            hetzner.createServer(
                recreatedUserData.render(),
                sshKey,
                volumes = listOf(dataVolume2.id),
            )
        recreatedServer.waitForSuccessfulProvisioning()

        Awaitility.await().atMost(5, TimeUnit.MINUTES).pollInterval(Duration.ofSeconds(5)).until {
            recreatedServer.host().portIsOpen(5432)
        }

        val recreatedJdbcUrl = "jdbc:postgresql://${recreatedServer.host}:5432/postgres"
        getAllDogNames(recreatedJdbcUrl, "superuser-very-secret")[0] shouldBe "snoopy"
    }

    fun createDatabaseAndInsertDog(jdbcUrl: String, password: String, username: String = "rds") {
        DriverManager.getConnection(jdbcUrl, username, password).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                CREATE TABLE IF NOT EXISTS dogs (
                    id SERIAL PRIMARY KEY,
                    name TEXT NOT NULL
                )
                    """.trimIndent(),
                )
                statement.executeUpdate("INSERT INTO dogs (name) VALUES ('snoopy')")
            }
        }
    }

    fun getAllDogNames(jdbcUrl: String, password: String, user: String = "rds"): List<String> {
        val names = mutableListOf<String>()

        DriverManager.getConnection(jdbcUrl, user, password).use { connection ->
            connection.createStatement().use { statement ->
                val resultSet = statement.executeQuery("SELECT name FROM dogs")
                while (resultSet.next()) {
                    names.add(resultSet.getString("name"))
                }
            }
        }

        return names
    }
}
