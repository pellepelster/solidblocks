package de.solidblocks.rds.postgresql.test

import mu.KotlinLogging
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File
import java.io.IOException
import java.sql.DriverManager
import java.time.Duration


class KDockerComposeContainer(file: File) : DockerComposeContainer<KDockerComposeContainer>(file)

class RdsPostgresqlIntegrationTest {

    private val logger = KotlinLogging.logger {}

    companion object {
        val host = "minio"
        val bucket = "database1-backup"
        val accessKey = "database1-user1"
        val secretKey = "ccbaa67e-cf26-432f-a11f-0c9e72abccf8"
        val database = "database1"
        val databaseUser = "user1"
        val databasePassword = "password1"
    }

    @Test
    fun testDatabaseStarts() {

        val minioCertificatePrivate =
            RdsPostgresqlIntegrationTest::class.java.getResource("/minio.key.pem").readText()
        val minioCertificatePublic =
            RdsPostgresqlIntegrationTest::class.java.getResource("/minio.pem").readText()
        val caPublic =
            RdsPostgresqlIntegrationTest::class.java.getResource("/ca.pem").readText()


        val dockerEnvironment = KDockerComposeContainer(File("src/test/resources/docker-compose.yml")).apply {
            withLogConsumer("minio", Slf4jLogConsumer(logger))
            withLogConsumer("postgresql", Slf4jLogConsumer(logger))
            withEnv(
                mapOf(
                    "MINIO_ADMIN_USER" to "admin12345",
                    "MINIO_ADMIN_PASSWORD" to "admin12345",
                    "MINIO_TLS_PRIVATE_KEY" to minioCertificatePrivate,
                    "MINIO_TLS_PUBLIC_KEY" to minioCertificatePublic,

                    "DB_BACKUP_S3_HOST" to host,
                    "DB_BACKUP_S3_BUCKET" to bucket,

                    "DB_BACKUP_S3_ACCESS_KEY" to accessKey,
                    "DB_BACKUP_S3_SECRET_KEY" to secretKey,
                    "DB_DATABASE" to database,
                    "DB_USERNAME" to databaseUser,
                    "DB_PASSWORD" to databasePassword,
                    "CA_PUBLIC_KEY" to caPublic,

                    "BUCKET_SPECS" to "${bucket}:${accessKey}:${secretKey}",
                )
            )
            withExposedService("postgresql", 5432)
            waitingFor("postgresql", Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
        }

        dockerEnvironment.start()

        val port = dockerEnvironment.getServicePort("postgresql", 5432)

        await.until {
            try {
                val url =
                    "jdbc:postgresql://${host}:$port/$database?user=$databaseUser&password=$databasePassword"

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
    }
}
