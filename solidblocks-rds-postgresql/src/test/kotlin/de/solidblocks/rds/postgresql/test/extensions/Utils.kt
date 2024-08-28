package de.solidblocks.rds.postgresql.test.extensions

import de.solidblocks.rds.postgresql.test.RdsPostgresqlMinioBackupIntegrationTest
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.time.Duration
import java.util.*
import mu.KotlinLogging
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.jdbi.v3.core.Jdbi
import org.testcontainers.containers.GenericContainer

val logger = KotlinLogging.logger {}

fun initWorldReadableTempDir(): File {
  val tempDir = "/tmp/temp-${UUID.randomUUID()}"

  File(tempDir).mkdirs()
  Files.setPosixFilePermissions(
      File(tempDir).toPath(), PosixFilePermissions.fromString("rwxrwxrwx"))

  return File(tempDir)
}

fun GenericContainer<out GenericContainer<*>>.createJdbi(
    username: String = RdsPostgresqlMinioBackupIntegrationTest.DATABASE_USER,
    password: String = RdsPostgresqlMinioBackupIntegrationTest.DATABASE_PASSWORD,
    database: String = RdsPostgresqlMinioBackupIntegrationTest.DATABASE,
): Jdbi {
  val port = this.getMappedPort(5432)
  return Jdbi.create(
      "jdbc:postgresql://localhost:$port/$database?user=$username&password=$password")
}

fun GenericContainer<out GenericContainer<*>>.createJdbiSSL(
    username: String = RdsPostgresqlMinioBackupIntegrationTest.DATABASE_USER,
    password: String = RdsPostgresqlMinioBackupIntegrationTest.DATABASE_PASSWORD,
    database: String = RdsPostgresqlMinioBackupIntegrationTest.DATABASE,
): Jdbi {
  val port = this.getMappedPort(5432)
  return Jdbi.create(
      "jdbc:postgresql://localhost:$port/$database?user=$username&password=$password&ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory")
}

fun GenericContainer<out GenericContainer<*>>.assertBackupFileHeaders(fileHeader: String) {
  val result = this.execInContainer("/test-dump-backup-file-headers.sh")
  Assertions.assertThat(result.stdout.lines()).hasSizeGreaterThan(4)
  Assertions.assertThat(result.stdout.lines().map { it.trim() }).allMatch {
    it.isBlank() || it == fileHeader
  }
}

fun Jdbi.createUserTable() {
  this.useHandle<RuntimeException> {
    it.execute("CREATE TABLE \"user\" (id VARCHAR PRIMARY KEY, \"name\" VARCHAR)")
  }
}

fun Jdbi.insertUser(name: String) {
  this.useHandle<RuntimeException> {
    it.createUpdate("INSERT INTO \"user\" (id, \"name\") VALUES (?, ?)")
        .bind(0, UUID.randomUUID())
        .bind(1, name)
        .execute()
  }
}

fun Jdbi.selectAllUsers(): List<Map<String, Any>>? =
    this.withHandle<List<Map<String, Any>>, RuntimeException> {
      it.createQuery("SELECT * FROM \"user\" ORDER BY \"name\"").mapToMap().list()
    }

fun Jdbi.assertHasUserWithName(name: String) {
  assertThat(this.selectAllUsers()).filteredOn { it["name"] == name }.hasSize(1)
}

fun Jdbi.waitForReady() {
  logger.info { "[test] waiting for postgres ready status" }

  await.atMost(Duration.ofMinutes(4)).until {
    try {
      this.useHandle<RuntimeException> { it.execute("select 1") == 1 }

      true
    } catch (e: Exception) {
      false
    }
  }
}

const val ENCRYPTED_FILE_HEADER = "53 61 6c 74 65 64 5f 5f"
const val COMPRESSED_FILE_HEADER = "1f 8b 08 00 00 00 00 00"

fun ByteArray.toHex(): String =
    joinToString(separator = " ") { eachByte -> "%02x".format(eachByte) }
