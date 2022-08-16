package de.solidblocks.rds.postgresql.test

import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import java.io.File
import java.io.IOException
import java.sql.DriverManager


class KDockerComposeContainer(file: File) : DockerComposeContainer<KDockerComposeContainer>(file)

class SolidBlocksRdsPostgresqlIntegrationTest {

    private val logger = KotlinLogging.logger {}

    companion object {
        val host = "localhost"
        val database = "database1"
        val username = "user1"
        val password = "password1"
    }

    @Test
    fun testDatabaseStarts() {

        val sshdHostKeyPrivate =
            SolidBlocksRdsPostgresqlIntegrationTest::class.java.getResource("/sshd_host_ed25519").readText()
        val sshdHostKeyPublic =
            SolidBlocksRdsPostgresqlIntegrationTest::class.java.getResource("/sshd_host_ed25519.pub").readText()

        val sshKeyPrivate =
            SolidBlocksRdsPostgresqlIntegrationTest::class.java.getResource("/postgresql_id_ed25519").readText()
        val sshKeyPublic =
            SolidBlocksRdsPostgresqlIntegrationTest::class.java.getResource("/postgresql_id_ed25519.pub").readText()

        val dockerEnvironment = KDockerComposeContainer(File("src/test/resources/docker-compose.yml")).apply {
            withLogConsumer("postgresql", Slf4jLogConsumer(logger))
            withLogConsumer("backup", Slf4jLogConsumer(logger))
            withEnv(
                mapOf(
                    "DB_DATABASE" to database,
                    "DB_USERNAME" to username,
                    "DB_PASSWORD" to password,
                    "DB_SSH_PRIVATE_KEY" to sshKeyPrivate,
                    "DB_SSH_PUBLIC_KEY" to sshKeyPublic,
                    "BACKUP_HOST" to "backup",
                    "BACKUP_HOST_PRIVATE_KEY" to sshdHostKeyPrivate,
                    "BACKUP_HOST_PUBLIC_KEY" to sshdHostKeyPublic,
                )
            )
            withExposedService("postgresql", 5432)
        }
        dockerEnvironment.start()

        val port = dockerEnvironment.getServicePort("postgresql", 5432)

        await.until {
            try {
                val url =
                    "jdbc:postgresql://${host}:$port/$database?user=$username&password=$password"

                DriverManager.getConnection(url).use { connection ->
                    connection.createStatement().use { statement ->
                        statement.executeQuery("SELECT 1").use { resultSet ->
                            return@until true
                        }
                    }
                }
            } catch (e: IOException) {
                logger.warn(e) { "postgres check failed for ${host}:${port}: '${e.message}'" }
            }

            false
        }

        assertThat(
            dockerEnvironment.getContainerByServiceName("postgresql_1").get()
                .execInContainer("ssh", "pgbackrest@backup", "whoami").stdout
        ).isEqualToIgnoringWhitespace("pgbackrest")
    }
}
