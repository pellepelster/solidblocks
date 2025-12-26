package de.solidblocks.rds.postgresql.test

import de.solidblocks.rds.postgresql.test.TestConstants.DATABASE
import de.solidblocks.rds.postgresql.test.extensions.*
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.util.*
import kotlin.io.path.pathString
import kotlin.io.path.writeText
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(RdsTestBedExtension::class)
@DisabledIfEnvironmentVariable(named = "SKIP_TESTS", matches = ".*integration.*")
class RdsPostgresqlConfigurationTest {

  val expectedExtensions =
      listOf(
          "adminpack",
          "amcheck",
          "autoinc",
          "bloom",
          "btree_gin",
          "btree_gist",
          "citext",
          "cube",
          "dblink",
          "dict_int",
          "dict_xsyn",
          "earthdistance",
          "file_fdw",
          "fuzzystrmatch",
          "hstore",
          "insert_username",
          "intagg",
          "intarray",
          "isn",
          "lo",
          "ltree",
          "moddatetime",
          "old_snapshot",
          "pageinspect",
          "pg_buffercache",
          "pg_cron",
          "pg_audit",
          "pg_freespacemap",
          "pg_prewarm",
          "pg_remote_exec",
          "pg_stat_statements",
          "pg_surgery",
          "pg_trgm",
          "pg_visibility",
          "pgcrypto",
          "pgrowlocks",
          "pgstattuple",
          "plpgsql",
          "postgres_fdw",
          "refint",
          "seg",
          "sslinfo",
          "tablefunc",
          "tcn",
          "tsm_system_rows",
          "tsm_system_time",
          "unaccent",
          "uuid-ossp",
          "xml2",
      )

  @Test
  fun testExecutesInitSql(testBed: RdsTestBed) {
    val dataDir = initWorldReadableTempDir()
    val localBackupDir = initWorldReadableTempDir()

    val initSqlFile = Files.createTempFile("init_sql", "sql")
    initSqlFile.writeText("CREATE TABLE \"table1\" (id VARCHAR PRIMARY KEY, \"name\" VARCHAR)")
    Files.setPosixFilePermissions(initSqlFile, PosixFilePermissions.fromString("rwxrwxrwx"))

    val container =
        testBed.createAndStartPostgresContainer(
            14,
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
                "DB_INIT_SQL_$DATABASE" to "/init_sql.sql",
            ),
            dataDir,
        ) {
          it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
          it.withFileSystemBind(initSqlFile.pathString, "/init_sql.sql")
        }

    with(testBed.logConsumer) { waitForLogLine("database system is ready to accept connections") }

    val name = UUID.randomUUID().toString()

    container.createJdbi().also {
      it.waitForReady()
      it.useHandle<RuntimeException> {
        it.createUpdate("INSERT INTO \"table1\" (id, \"name\") VALUES (?, ?)")
            .bind(0, UUID.randomUUID())
            .bind(1, name)
            .execute()
      }

      it.useHandle<RuntimeException> {
        val result = it.createQuery("SELECT * FROM \"table1\" ORDER BY \"name\"").mapToMap().list()

        assertThat(result).filteredOn { it["name"] == name }.hasSize(1)
      }
    }

    container.stop()
  }

  @Test
  fun testCreateDefault(testBed: RdsTestBed) {
    val dataDir = initWorldReadableTempDir()
    val localBackupDir = initWorldReadableTempDir()

    val container =
        testBed.createAndStartPostgresContainer(
            14,
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
            ),
            dataDir,
        ) {
          it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }

    with(testBed.logConsumer) { waitForLogLine("database system is ready to accept connections") }

    // ensure defaults if no database options are set
    container.createJdbi().also {
      it.waitForReady()
      it.useHandle<RuntimeException> {
        val databaseInfo =
            it.createQuery(
                    "SELECT pg_encoding_to_char(encoding) as encoding, datcollate, datctype FROM pg_database WHERE datname = '$DATABASE';",
                )
                .mapToMap()
                .list()
                .first()
        databaseInfo["encoding"] shouldBe "UTF8"
        databaseInfo["datcollate"] shouldBe "en_US.UTF-8"
        databaseInfo["datctype"] shouldBe "en_US.UTF-8"
      }
    }

    container.stop()
  }

  @Test
  fun testCreateDatabaseOptions(testBed: RdsTestBed) {
    val dataDir = initWorldReadableTempDir()
    val localBackupDir = initWorldReadableTempDir()

    val container =
        testBed.createAndStartPostgresContainer(
            14,
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
                "DB_CREATE_OPTIONS_$DATABASE" to
                    "ENCODING='UTF8' LC_COLLATE='de_DE.UTF-8' LC_CTYPE='de_DE.UTF-8' TEMPLATE='template0'",
            ),
            dataDir,
        ) {
          it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }

    with(testBed.logConsumer) { waitForLogLine("database system is ready to accept connections") }

    container.createJdbi().also {
      it.waitForReady()
      it.useHandle<RuntimeException> {
        val databaseInfo =
            it.createQuery(
                    "SELECT pg_encoding_to_char(encoding) as encoding, datcollate, datctype FROM pg_database WHERE datname = '$DATABASE';",
                )
                .mapToMap()
                .list()
                .first()
        databaseInfo["encoding"] shouldBe "UTF8"
        databaseInfo["datcollate"] shouldBe "de_DE.UTF-8"
        databaseInfo["datctype"] shouldBe "de_DE.UTF-8"
      }
    }
    container.stop()
  }

  @Test
  fun testSSL(testBed: RdsTestBed) {
    val dataDir = initWorldReadableTempDir()
    val localBackupDir = initWorldReadableTempDir()

    val sslServerKey =
        Base64.getEncoder()
            .encodeToString(
                TestConstants::class.java.getResource("/rds.key.pem").readBytes(),
            )

    val sslServerCert =
        Base64.getEncoder()
            .encodeToString(
                TestConstants::class.java.getResource("/rds.pem").readBytes(),
            )

    val container =
        testBed.createAndStartPostgresContainer(
            14,
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
                "SSL_SERVER_CERT" to sslServerCert,
                "SSL_SERVER_KEY" to sslServerKey,
            ),
            dataDir,
        ) {
          it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }

    with(testBed.logConsumer) { waitForLogLine("database system is ready to accept connections") }

    val username = UUID.randomUUID().toString()
    container.createJdbiSSL().also {
      it.waitForReady()
      it.createUserTable()
      it.insertUser(username)
      it.assertHasUserWithName(username)
    }

    container.stop()
  }

  @Test
  fun testIgnoresUnreadableInitSql(testBed: RdsTestBed) {
    val dataDir = initWorldReadableTempDir()
    val localBackupDir = initWorldReadableTempDir()

    val initSqlFile = Files.createTempFile("init_sql", "sql")
    initSqlFile.writeText("CREATE TABLE \"table1\" (id VARCHAR PRIMARY KEY, \"name\" VARCHAR)")

    val container =
        testBed.createAndStartPostgresContainer(
            14,
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
                "DB_INIT_SQL_$DATABASE" to "/init_sql.sql",
            ),
            dataDir,
        ) {
          it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
          it.withFileSystemBind(initSqlFile.pathString, "/init_sql.sql")
        }

    with(testBed.logConsumer) { waitForLogLine("database system is ready to accept connections") }

    container.stop()
  }

  @Test
  fun testMaintenanceMode(testBed: RdsTestBed) {
    val dataDir = initWorldReadableTempDir()
    val localBackupDir = initWorldReadableTempDir()

    val container =
        testBed.createAndStartPostgresContainer(
            14,
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
            ),
            dataDir,
        ) {
          it.withExposedPorts(*arrayOf<Int>())
          it.withCommand("maintenance")
          it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }

    with(testBed.logConsumer) {
      waitForLogLine("maintenance mode is active, no database is started")
    }

    container.stop()
  }

  @Test
  fun testChangesUsernameAndPasswordAfterRestart(rdsTestBed: RdsTestBed) {
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

    val username = UUID.randomUUID().toString()

    container.createJdbi().also {
      it.waitForReady()
      it.createUserTable()
      it.insertUser(username)
      it.assertHasUserWithName(username)
    }

    container.stop()
    rdsTestBed.logConsumer.clear()
    container
        .withEnv(
            mapOf(
                "DB_USERNAME_$DATABASE" to "new-user",
                "DB_PASSWORD_$DATABASE" to "new-password",
            ),
        )
        .start()

    with(rdsTestBed.logConsumer) {
      // on second start with persistent storage no initializing ord backup should be executed
      waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
      assertHasLogLine("[solidblocks-rds-postgresql] data dir is not empty")
      assertHasNoLogLine("[solidblocks-rds-postgresql] initializing database instance")
      assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
      assertHasNoLogLine("[solidblocks-rds-postgresql] executing initial backup")
      waitForLogLine("database system is ready to accept connections")
    }

    container.createJdbi("new-user", "new-password").also {
      it.waitForReady()
      it.assertHasUserWithName(username)
    }

    container.stop()
  }

  @Test
  fun testChangesUsernameAndPasswordAfterRestore(rdsTestBed: RdsTestBed) {
    val localBackupDir = initWorldReadableTempDir()
    val container1 =
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

    val username = UUID.randomUUID().toString()

    container1.createJdbi().also {
      it.waitForReady()
      it.createUserTable()
      it.insertUser(username)
      it.assertHasUserWithName(username)
    }

    container1.execInContainer("backup-full.sh")
    container1.stop()
    rdsTestBed.logConsumer.clear()

    val container2 =
        rdsTestBed.createAndStartPostgresContainer(
            14,
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
                "DB_USERNAME_$DATABASE" to "new-user",
                "DB_PASSWORD_$DATABASE" to "new-password",
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

    container2.createJdbi("new-user", "new-password").also {
      it.waitForReady()
      it.assertHasUserWithName(username)
    }

    container2.stop()
  }

  @Test
  fun testSetCustomPostgresConfig(rdsTestBed: RdsTestBed) {
    val dataDir = initWorldReadableTempDir()
    val localBackupDir = initWorldReadableTempDir()

    val container =
        rdsTestBed.createAndStartPostgresContainer(
            14,
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
                "DB_POSTGRES_EXTRA_CONFIG" to
                    listOf("checkpoint_timeout = 301", "archive_timeout = 123").joinToString("\n"),
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

    val settings =
        container.createJdbi().withHandle<List<Map<String, Any>>, RuntimeException> {
          it.createQuery("SELECT * FROM \"pg_settings\" ORDER BY \"name\"").mapToMap().list()
        }
    assertThat(settings.first { it["name"] == "checkpoint_timeout" }["setting"]).isEqualTo("301")
    assertThat(settings.first { it["name"] == "archive_timeout" }["setting"]).isEqualTo("123")

    val extensions =
        container.createJdbi().withHandle<List<Map<String, Any>>, RuntimeException> {
          it.createQuery("SELECT * FROM \"pg_available_extensions\" ORDER BY \"name\"")
              .mapToMap()
              .list()
        }

    println("=========== installed extensions =========== ")
    extensions.forEach {
      println("| ${it["name"]} | ${it["default_version"]} | ${it["comment"]} |")
    }

    expectedExtensions.forEach { expectedExtension ->
      assertThat(extensions.any { it["name"] == expectedExtension })
    }

    container.stop()
  }

  @Test
  fun testSetCustomPostgresConfigWithEscapedNewlines(rdsTestBed: RdsTestBed) {
    val dataDir = initWorldReadableTempDir()
    val localBackupDir = initWorldReadableTempDir()

    val container =
        rdsTestBed.createAndStartPostgresContainer(
            14,
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
                "DB_POSTGRES_EXTRA_CONFIG" to "checkpoint_timeout = 301\\narchive_timeout = 123",
            ),
            dataDir,
        ) {
          it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }

    with(rdsTestBed.logConsumer) {
      waitForLogLine("database system is ready to accept connections")
    }

    val settings =
        container.createJdbi().withHandle<List<Map<String, Any>>, RuntimeException> {
          it.createQuery("SELECT * FROM \"pg_settings\" ORDER BY \"name\"").mapToMap().list()
        }

    assertThat(settings.first { it["name"] == "checkpoint_timeout" }["setting"]).isEqualTo("301")
    assertThat(settings.first { it["name"] == "archive_timeout" }["setting"]).isEqualTo("123")

    container.stop()
  }

  @Test
  fun testHasCreateSchemaPermissions(testBed: RdsTestBed) {
    val dataDir = initWorldReadableTempDir()
    val localBackupDir = initWorldReadableTempDir()

    val container =
        testBed.createAndStartPostgresContainer(
            14,
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
            ),
            dataDir,
        ) {
          it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }

    with(testBed.logConsumer) { waitForLogLine("database system is ready to accept connections") }

    container.createJdbi().useHandle<Exception> { it.execute("CREATE SCHEMA myschema;") }

    container.stop()
  }

  @Test
  fun testHasAdminPassword(rdsTestBed: RdsTestBed) {
    val dataDir = initWorldReadableTempDir()
    val localBackupDir = initWorldReadableTempDir()

    val container1 =
        rdsTestBed.createAndStartPostgresContainer(
            14,
            mapOf(
                "DB_ADMIN_PASSWORD" to "my-database-password",
                "DB_BACKUP_LOCAL" to "1",
            ),
            dataDir,
        ) {
          it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }

    with(rdsTestBed.logConsumer) {
      waitForLogLine("database system is ready to accept connections")
    }

    container1.createJdbi("rds", "my-database-password").useHandle<Exception> {
      it.execute("SELECT version()")
    }

    container1.stop()
    rdsTestBed.logConsumer.clear()

    val container2 =
        rdsTestBed.createAndStartPostgresContainer(
            14,
            mapOf(
                "DB_ADMIN_PASSWORD" to "new-database-password",
                "DB_BACKUP_LOCAL" to "1",
            ),
            dataDir,
        ) {
          it.withFileSystemBind(localBackupDir.absolutePath, "/storage/backup")
        }

    with(rdsTestBed.logConsumer) {
      waitForLogLine("database system is ready to accept connections")
    }

    container2.createJdbi("rds", "new-database-password").useHandle<Exception> {
      it.execute("SELECT version()")
    }

    container2.stop()
  }

  @Test
  fun testCreatesExtraDatabases(rdsTestBed: RdsTestBed) {
    val dataDir = initWorldReadableTempDir()
    val localBackupDir = initWorldReadableTempDir()

    val container =
        rdsTestBed.createAndStartPostgresContainer(
            14,
            mapOf(
                "DB_BACKUP_LOCAL" to "1",
                "DB_DATABASE_extra_database_1" to "extra-database-1",
                "DB_USERNAME_extra_database_1" to "extra-user-1",
                "DB_PASSWORD_extra_database_1" to "extra-password-1",
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

    container.createJdbi("extra-user-1", "extra-password-1", "extra-database-1").also {
      it.waitForReady()
    }

    container.stop()
  }
}
