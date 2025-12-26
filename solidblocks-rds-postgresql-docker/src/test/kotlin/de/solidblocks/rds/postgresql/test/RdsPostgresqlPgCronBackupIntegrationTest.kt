package de.solidblocks.rds.postgresql.test

import de.solidblocks.rds.postgresql.test.extensions.*
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@ExtendWith(RdsTestBedExtension::class)
@DisabledIfEnvironmentVariable(named = "SKIP_TESTS", matches = ".*integration.*")
class RdsPostgresqlPgCronBackupIntegrationTest {

  @ParameterizedTest
  @ValueSource(ints = [16])
  fun testRestoreDatabaseFromFullBackup(version: Int, rdsTestBed: RdsTestBed) {
    val localBackupDir = initWorldReadableTempDir()
    val postgresContainer =
        rdsTestBed.createAndStartPostgresContainer(
            version,
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
                "DB_BACKUP_INCR_SCHEDULE" to "* * * * *",
            ),
            initWorldReadableTempDir(),
        ) {
          it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }

    with(rdsTestBed.logConsumer) {
      waitForLogLine(
          "cron job 1 starting: select pg_remote_exec_fetch('/rds/bin/backup-incr.sh','t')",
      )
    }

    postgresContainer.stop()
  }
}
