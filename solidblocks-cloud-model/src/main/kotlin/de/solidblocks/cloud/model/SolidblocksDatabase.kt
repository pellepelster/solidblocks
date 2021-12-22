package de.solidblocks.cloud.model

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import liquibase.Contexts
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.exception.DatabaseException
import liquibase.resource.FileSystemResourceAccessor
import mu.KotlinLogging
import org.jooq.impl.DataSourceConnectionProvider
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.DefaultDSLContext
import java.nio.file.Files
import java.sql.SQLException
import javax.sql.DataSource
import kotlin.io.path.writeText

class SolidblocksDatabase(jdbcUrl: String) {

    private val logger = KotlinLogging.logger {}

    val dsl: DefaultDSLContext

    private val datasource: DataSource

    init {

        logger.info { "initializing database '$jdbcUrl'" }

        val config = HikariConfig()
        config.jdbcUrl = jdbcUrl

        datasource = HikariDataSource(config)

        val ds = DataSourceConnectionProvider(datasource)
        val configuration = DefaultConfiguration()
        configuration.set(ds)

        dsl = DefaultDSLContext(configuration)
    }

    fun ensureDBSchema() {
        try {
            val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(datasource.connection))

            val changelogContent = SolidblocksDatabase::class.java.getResource("/db/changelog/db.changelog-master.yaml").readText()
            val changelogDir = Files.createTempDirectory("solidblocks")
            val changelogFile = Files.createTempFile(changelogDir, "changelog", ".yml")
            changelogFile.writeText(changelogContent)

            val liquibase = Liquibase(changelogFile.toFile().toString(), FileSystemResourceAccessor(changelogDir.toFile()), database)
            liquibase.log
            liquibase.update(Contexts())
        } catch (e: SQLException) {
            throw DatabaseException(e)
        } finally {
            try {
                datasource.connection.rollback()
                datasource.connection.close()
            } catch (e: SQLException) {
                // nothing to do
            }
        }
    }
}
