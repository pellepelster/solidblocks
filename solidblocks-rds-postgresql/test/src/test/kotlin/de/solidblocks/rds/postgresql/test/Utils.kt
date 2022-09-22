package de.solidblocks.rds.postgresql.test

import mu.KotlinLogging
import org.awaitility.kotlin.await
import org.jdbi.v3.core.Jdbi
import org.testcontainers.containers.GenericContainer
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.util.*

private val logger = KotlinLogging.logger {}

fun initWorldReadableTempDir(): File {
    val tempDir = "/tmp/temp-${UUID.randomUUID()}"

    File(tempDir).mkdirs()
    Files.setPosixFilePermissions(File(tempDir).toPath(), PosixFilePermissions.fromString("rwxrwxrwx"))

    return File(tempDir)
}

fun GenericContainer<out GenericContainer<*>>.createJdbi(username: String = RdsPostgresqlMinioBackupIntegrationTest.databaseUser, password: String = RdsPostgresqlMinioBackupIntegrationTest.databasePassword, database: String = RdsPostgresqlMinioBackupIntegrationTest.database): Jdbi {
    val port = this.getMappedPort(5432)
    return Jdbi.create("jdbc:postgresql://localhost:$port/${database}?user=${username}&password=${password}")
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

fun Jdbi.selectAllUsers(): List<Map<String, Any>>? {
    return this.withHandle<List<Map<String, Any>>, RuntimeException> {
        it.createQuery("SELECT * FROM \"user\" ORDER BY \"name\"")
                .mapToMap()
                .list()
    }
}

fun Jdbi.waitForReady() {
    await.until {
        try {
            this.useHandle<RuntimeException> {
                it.execute("select 1") == 1
            }

            true
        } catch (e: Exception) {
            false
        }

    }
}