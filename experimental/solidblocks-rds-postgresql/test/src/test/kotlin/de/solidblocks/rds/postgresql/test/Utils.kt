package de.solidblocks.rds.postgresql.test

import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
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

fun GenericContainer<out GenericContainer<*>>.createJdbi(): Jdbi {
    val port = this.getMappedPort(5432)
    return Jdbi.create("jdbc:postgresql://localhost:$port/${RdsPostgresqlIntegrationTest.database}?user=${RdsPostgresqlIntegrationTest.databaseUser}&password=${RdsPostgresqlIntegrationTest.databasePassword}")
}
