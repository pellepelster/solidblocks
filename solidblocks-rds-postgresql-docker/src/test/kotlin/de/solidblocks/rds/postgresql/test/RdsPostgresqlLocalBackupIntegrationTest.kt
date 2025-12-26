package de.solidblocks.rds.postgresql.test

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.command.LogContainerResultCallback
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import de.solidblocks.rds.postgresql.test.extensions.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import java.io.IOException
import java.net.URI
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@ExtendWith(RdsTestBedExtension::class)
@DisabledIfEnvironmentVariable(named = "SKIP_TESTS", matches = ".*integration.*")
class RdsPostgresqlLocalBackupIntegrationTest {

  val dockerClient: DockerClient =
      DockerClientBuilder.getInstance()
          .withDockerHttpClient(
              ZerodepDockerHttpClient.Builder()
                  .dockerHost(URI.create("unix:///var/run/docker.sock"))
                  .build(),
          )
          .build()

  companion object {
    val database = "database1"
  }

  @Test
  fun testDatabaseBackupEncryption(rdsTestBed: RdsTestBed) {
    val localBackupDir = initWorldReadableTempDir()
    val postgresContainer1 =
        rdsTestBed.createAndStartPostgresContainer(
            14,
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
                "DB_BACKUP_ENCRYPTION_PASSPHRASE" to "yolo123",
            ),
            initWorldReadableTempDir(),
        ) {
          it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }

    with(rdsTestBed.logConsumer) {
      waitForLogLine("database system is ready to accept connections")
    }

    val username = UUID.randomUUID().toString()

    postgresContainer1.createJdbi().also {
      it.waitForReady()
      it.createUserTable()
      it.insertUser(username)
      it.assertHasUserWithName(username)
    }

    postgresContainer1.execInContainer("backup-full.sh")
    postgresContainer1.assertBackupFileHeaders(ENCRYPTED_FILE_HEADER)

    postgresContainer1.stop()
    rdsTestBed.logConsumer.clear()

    val postgresContainer2 =
        rdsTestBed.createAndStartPostgresContainer(
            14,
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
                "DB_BACKUP_ENCRYPTION_PASSPHRASE" to "yolo123",
            ),
            initWorldReadableTempDir(),
        ) {
          it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }

    with(rdsTestBed.logConsumer) {
      waitForLogLine("database system is ready to accept connections")
    }

    postgresContainer2.createJdbi().also {
      it.waitForReady()
      it.assertHasUserWithName(username)
    }

    postgresContainer2.stop()
  }

  @Test
  fun testDatabaseKeepsDataBetweenRestarts(rdsTestBed: RdsTestBed) {
    val dataDir = initWorldReadableTempDir()
    val localBackupDir = initWorldReadableTempDir()

    val container =
        rdsTestBed.createAndStartPostgresContainer(
            14,
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
            ),
            dataDir,
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

    container.execInContainer("rds_provisioning_completed.sh").exitCode shouldBe 0

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

  @Test
  fun testMigratesOldDirectoryLayout(rdsTestBed: RdsTestBed) {
    val dataDir = initWorldReadableTempDir()
    val localBackupDir = initWorldReadableTempDir()

    val oldContainer =
        rdsTestBed.createAndStartPostgresContainer(
            "ghcr.io/pellepelster/solidblocks-rds-postgresql:v0.1.17",
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
            ),
            dataDir,
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

    val newContainer14 =
        rdsTestBed.createAndStartPostgresContainer(
            14,
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
            ),
            dataDir,
        ) {
          it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }
    newContainer14.start()

    with(rdsTestBed.logConsumer) {
      // on second start with persistent storage no initializing ord backup should be executed
      waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
      assertHasLogLine(
          "[solidblocks-rds-postgresql] old directory layout detected, migrating data files from '/storage/data/database1' to '/storage/data/database1/14'",
      )
      assertHasLogLine("[solidblocks-rds-postgresql] data dir is not empty")
      assertHasNoLogLine("[solidblocks-rds-postgresql] initializing database instance")
      assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
      assertHasNoLogLine("[solidblocks-rds-postgresql] executing initial backup")
      waitForLogLine("database system is ready to accept connections")
    }

    newContainer14.createJdbi().also {
      it.waitForReady()
      it.assertHasUserWithName(username)
    }

    newContainer14.stop()
  }

  fun runCommand(image: String, vararg args: String): String {
    dockerClient.pullImageCmd(image).start().awaitCompletion()

    val container = dockerClient.createContainerCmd(image).withEntrypoint(args.toList()).exec()

    dockerClient.startContainerCmd(container.id).exec()
    dockerClient.waitContainerCmd(container.id).start().awaitStatusCode()

    val sb = StringBuilder()
    dockerClient
        .logContainerCmd(container.id)
        .withStdOut(true)
        .withStdErr(true)
        .withFollowStream(true)
        .exec(
            object : LogContainerResultCallback() {
              public override fun onNext(frame: Frame) {
                try {
                  sb.append(frame.payload.toString(Charsets.UTF_8))
                } catch (e: IOException) {
                  throw RuntimeException(e)
                }
                super.onNext(frame)
              }
            },
        )
        .awaitCompletion()
    dockerClient.removeContainerCmd(container.id).withForce(true).exec()

    return sb.toString()
  }

  @ParameterizedTest
  @ValueSource(ints = [14, 15, 16, 17, 18])
  fun checkTools(version: Int, rdsTestBed: RdsTestBed) {
    runCommand(
        "ghcr.io/pellepelster/solidblocks-rds-postgresql:$version-${
                System.getenv("VERSION") ?: "snapshot"
            }",
        "psql",
        "--version",
    ) shouldStartWith "psql (PostgreSQL) $version"

    runCommand(
        "ghcr.io/pellepelster/solidblocks-rds-postgresql:$version-${
                System.getenv("VERSION") ?: "snapshot"
            }",
        "uuidgen",
        "--version",
    ) shouldStartWith "uuidgen from util-linux"
  }

  @ParameterizedTest
  @ValueSource(ints = [14, 15, 16, 17, 18])
  fun testRestoreDatabaseFromFullBackup(version: Int, rdsTestBed: RdsTestBed) {
    val localBackupDir = initWorldReadableTempDir()
    val postgresContainer1 =
        rdsTestBed.createAndStartPostgresContainer(
            version,
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
            ),
            initWorldReadableTempDir(),
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

    postgresContainer1.createJdbi().also {
      it.waitForReady()
      it.createUserTable()
      it.insertUser(username)
      it.assertHasUserWithName(username)
    }

    postgresContainer1.execInContainer("backup-full.sh")
    postgresContainer1.assertBackupFileHeaders(COMPRESSED_FILE_HEADER)

    postgresContainer1.stop()
    rdsTestBed.logConsumer.clear()

    val postgresContainer2 =
        rdsTestBed.createAndStartPostgresContainer(
            version,
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
            ),
            initWorldReadableTempDir(),
        ) {
          it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }

    with(rdsTestBed.logConsumer) {
      // on second start without persistent storage restore should be executed
      waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
      assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
      assertHasNoLogLine("[solidblocks-rds-postgresql] initializing database instance")
      assertHasLogLine("[solidblocks-rds-postgresql] restoring database from backup")
      assertHasNoLogLine("[solidblocks-rds-postgresql] executing initial backup")
      waitForLogLine("database system is ready to accept connections")
    }

    postgresContainer2.createJdbi().also {
      it.waitForReady()
      it.assertHasUserWithName(username)
    }

    postgresContainer2.stop()
  }

  @ParameterizedTest
  @ValueSource(ints = [14, 15])
  fun testRestoreDatabasePitr(version: Int, rdsTestBed: RdsTestBed) {
    val localBackupDir = initWorldReadableTempDir()
    val checkpointTimeout = 30L

    val postgresContainer1 =
        rdsTestBed.createAndStartPostgresContainer(
            version,
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
                "DB_POSTGRES_EXTRA_CONFIG" to "checkpoint_timeout = $checkpointTimeout",
            ),
            initWorldReadableTempDir(),
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

    val username1 = UUID.randomUUID().toString()
    val username2 = UUID.randomUUID().toString()
    val username3 = UUID.randomUUID().toString()

    postgresContainer1.createJdbi().also {
      it.waitForReady()
      it.createUserTable()
      it.insertUser(username1)
      it.assertHasUserWithName(username1)
    }

    postgresContainer1.execInContainer("backup-incr.sh")

    postgresContainer1.createJdbi().also {
      it.insertUser(username2)
      it.assertHasUserWithName(username2)
    }

    Thread.sleep((checkpointTimeout + 10) * 1000)
    val user2Timestamp = Instant.now()

    postgresContainer1.execInContainer("backup-incr.sh")
    postgresContainer1.createJdbi().also {
      it.insertUser(username3)
      it.assertHasUserWithName(username3)
    }

    postgresContainer1.execInContainer("backup-incr.sh")
    postgresContainer1.stop()
    rdsTestBed.logConsumer.clear()

    val formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC)

    val postgresContainer2 =
        rdsTestBed.createAndStartPostgresContainer(
            version,
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
                "DB_RESTORE_PITR" to formatter.format(user2Timestamp),
            ),
            initWorldReadableTempDir(),
        ) {
          it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }

    with(rdsTestBed.logConsumer) {
      // on second start without persistent storage restore should be executed
      waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
      assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
      assertHasNoLogLine("[solidblocks-rds-postgresql] initializing database instance")
      assertHasLogLine("[solidblocks-rds-postgresql] restoring database from backup")
      assertHasNoLogLine("[solidblocks-rds-postgresql] executing initial backup")
      waitForLogLine("database system is ready to accept connections")
    }

    postgresContainer2.createJdbi().also {
      it.waitForReady()
      it.assertHasUserWithName(username1)
      it.assertHasUserWithName(username2)

      assertThat(it.selectAllUsers()).filteredOn { it["name"] == username3 }.hasSize(0)
    }

    postgresContainer2.stop()
  }

  @Test
  fun testRestoreDatabaseFromDifferentialBackup(rdsTestBed: RdsTestBed) {
    val localBackupDir = initWorldReadableTempDir()
    val postgresContainer1 =
        rdsTestBed.createAndStartPostgresContainer(
            14,
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
            ),
            initWorldReadableTempDir(),
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

    val username1 = UUID.randomUUID().toString()
    postgresContainer1.createJdbi().also {
      it.waitForReady()
      it.createUserTable()
      it.insertUser(username1)
      it.assertHasUserWithName(username1)
    }

    postgresContainer1.execInContainer("backup-full.sh")

    val username2 = UUID.randomUUID().toString()
    postgresContainer1.createJdbi().also {
      it.insertUser(username2)
      it.assertHasUserWithName(username2)
    }

    postgresContainer1.execInContainer("backup-diff.sh")

    postgresContainer1.stop()
    rdsTestBed.logConsumer.clear()

    val postgresContainer2 =
        rdsTestBed.createAndStartPostgresContainer(
            14,
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
            ),
            initWorldReadableTempDir(),
        ) {
          it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }

    with(rdsTestBed.logConsumer) {
      // on second start without persistent storage restore should be executed
      waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
      assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
      assertHasNoLogLine("[solidblocks-rds-postgresql] initializing database instance")
      assertHasLogLine("[solidblocks-rds-postgresql] restoring database from backup")
      assertHasNoLogLine("[solidblocks-rds-postgresql] executing initial backup")
      waitForLogLine("database system is ready to accept connections")
    }

    postgresContainer2.createJdbi().also {
      it.waitForReady()
      it.assertHasUserWithName(username1)
      it.assertHasUserWithName(username2)
    }

    postgresContainer2.stop()
  }
}
