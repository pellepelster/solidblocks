package de.solidblocks.rds.postgresql.test

import de.solidblocks.rds.postgresql.test.RdsPostgresqlMinioBackupIntegrationTest.Companion.database
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import java.util.*


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RdsTestBedExtension::class)
@Disabled
class RdsPostgresqlInvalidConfigIntegrationTest {

    private val logger = KotlinLogging.logger {}

    companion object {
        val backupHost = "minio"
        val bucket = "database1-backup"
        val accessKey = "database1-user1"
        val secretKey = "ccbaa67e-cf26-432f-a11f-0c9e72abccf8"
        val database = "database1"
        val databaseUser = "user1"
        val databasePassword = "password1"

        val caPublicBase64 =
                Base64.getEncoder().encodeToString(
                        RdsPostgresqlInvalidConfigIntegrationTest::class.java.getResource("/ca.pem").readBytes()
                )

    }

    @Test
    fun doesNotStartIfNoDataDirIsMounted() {

        val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))

        val container = GenericContainer("solidblocks-rds-postgresql").apply {
            withLogConsumer(logConsumer)
            withEnv(
                    mapOf(
                            "DB_BACKUP_S3" to "1",
                            "DB_BACKUP_S3_HOST" to backupHost,
                            "DB_BACKUP_S3_BUCKET" to bucket,
                            "DB_BACKUP_S3_ACCESS_KEY" to accessKey,
                            "DB_BACKUP_S3_SECRET_KEY" to secretKey,

                            "DB_INSTANCE_NAME" to database,
                            "DB_DATABASE_${RdsPostgresqlMinioBackupIntegrationTest.database}" to database,
                            "DB_USERNAME_${RdsPostgresqlMinioBackupIntegrationTest.database}" to databaseUser,
                            "DB_PASSWORD_${RdsPostgresqlMinioBackupIntegrationTest.database}" to databasePassword,

                            "DB_BACKUP_S3_CA_PUBLIC_KEY" to caPublicBase64,
                    )
            )
        }

        Assertions.assertThrows(ContainerLaunchException::class.java) {
            container.start()
        }

        logConsumer.waitForLogLine("[solidblocks-rds-postgresql] storage dir '/storage/data' not mounted")
    }

    @Test
    fun doesNotStartIfNoBackupMethodSelected(rdsTestBed: RdsTestBed) {

        val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))

        Assertions.assertThrows(ContainerLaunchException::class.java) {
            rdsTestBed.createAndStartPostgresContainer(
                    mapOf(
                    ), initWorldReadableTempDir(), logConsumer
            )
        }

        logConsumer.waitForLogLine("[solidblocks-rds-postgresql] either 'DB_BACKUP_S3' or 'DB_BACKUP_LOCAL' has to be activated")
    }

    @Test
    fun doesNotStartIfNoBackupDirMounted(rdsTestBed: RdsTestBed) {

        val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))

        Assertions.assertThrows(ContainerLaunchException::class.java) {
            rdsTestBed.createAndStartPostgresContainer(
                    mapOf(
                            "DB_BACKUP_LOCAL" to "1"
                    ), initWorldReadableTempDir(), logConsumer
            )
        }

        logConsumer.waitForLogLine("[solidblocks-rds-postgresql] local backup dir '/storage/backup' not mounted")
    }

}
