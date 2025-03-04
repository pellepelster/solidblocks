package de.solidblocks.rds.postgresql.test

import de.solidblocks.rds.postgresql.test.extensions.RdsTestBed
import de.solidblocks.rds.postgresql.test.extensions.RdsTestBedExtension
import de.solidblocks.rds.postgresql.test.extensions.TestContainersLogConsumer
import de.solidblocks.rds.postgresql.test.extensions.initWorldReadableTempDir
import de.solidblocks.rds.postgresql.test.extensions.logger
import java.util.*
import org.junit.Assert.assertThrows
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.images.PullPolicy

@ExtendWith(RdsTestBedExtension::class)
@Disabled
class RdsPostgresqlInvalidConfigIntegrationTest {

  companion object {
    val backupHost = "minio"
    val bucket = "database1-backup"
    val accessKey = "database1-user1"
    val secretKey = "ccbaa67e-cf26-432f-a11f-0c9e72abccf8"
    val database = "database1"
    val databaseUser = "user1"
    val databasePassword = "password1"

    val caPublicBase64 =
        Base64.getEncoder()
            .encodeToString(
                RdsPostgresqlInvalidConfigIntegrationTest::class
                    .java
                    .getResource("/ca.pem")
                    .readBytes(),
            )
  }

  @Test
  fun doesNotStartIfNoDataDirIsMounted() {
    val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))

    val container =
        GenericContainer(
                "ghcr.io/pellepelster/solidblocks-rds-postgresql:${System.getenv("VERSION") ?: "snapshot"}-rc",
            )
            .apply {
              withImagePullPolicy(PullPolicy.alwaysPull())
              withLogConsumer(logConsumer)
              withEnv(
                  mapOf(
                      "DB_BACKUP_S3" to "1",
                      "DB_BACKUP_S3_HOST" to backupHost,
                      "DB_BACKUP_S3_BUCKET" to bucket,
                      "DB_BACKUP_S3_ACCESS_KEY" to accessKey,
                      "DB_BACKUP_S3_SECRET_KEY" to secretKey,
                      "DB_INSTANCE_NAME" to database,
                      "DB_DATABASE_${TestConstants.DATABASE}" to database,
                      "DB_USERNAME_${TestConstants.DATABASE}" to
                          databaseUser,
                      "DB_PASSWORD_${TestConstants.DATABASE}" to
                          databasePassword,
                      "DB_BACKUP_S3_CA_PUBLIC_KEY" to caPublicBase64,
                  ),
              )
            }

    Assertions.assertThrows(ContainerLaunchException::class.java) { container.start() }

    logConsumer.waitForLogLine(
        "[solidblocks-rds-postgresql] storage dir '/storage/data' not mounted",
    )

    container.stop()
  }

  @Test
  fun doesNotStartIfNoBackupMethodSelected(rdsTestBed: RdsTestBed) {
    assertThrows(ContainerLaunchException::class.java) {
      rdsTestBed.createAndStartPostgresContainer(
          14,
          mapOf(),
          initWorldReadableTempDir(),
      )
    }

    with(rdsTestBed.logConsumer) {
      waitForLogLine(
          "[solidblocks-rds-postgresql] either 'DB_BACKUP_S3' or 'DB_BACKUP_LOCAL' has to be activated",
      )
    }
  }

  @Test
  fun doesNotStartIfNoBackupDirMounted(rdsTestBed: RdsTestBed) {
    assertThrows(ContainerLaunchException::class.java) {
      rdsTestBed.createAndStartPostgresContainer(
          14,
          mapOf(
              "DB_BACKUP_LOCAL" to "1",
          ),
          initWorldReadableTempDir(),
      )
    }

    with(rdsTestBed.logConsumer) {
      waitForLogLine("[solidblocks-rds-postgresql] local backup dir '/storage/backup' not mounted")
    }
  }
}
