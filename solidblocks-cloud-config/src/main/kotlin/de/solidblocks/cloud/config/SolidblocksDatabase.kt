package de.solidblocks.cloud.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import liquibase.Contexts
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.exception.DatabaseException
import liquibase.resource.ClassLoaderResourceAccessor
import org.jooq.impl.DataSourceConnectionProvider
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.DefaultDSLContext
import java.sql.SQLException
import javax.sql.DataSource

class SolidblocksDatabase(jdbcUrl: String) {

    val dsl: DefaultDSLContext

    private val datasource: DataSource

    init {
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
            val liquibase = Liquibase("classpath:/db/changelog/db.changelog-master.yaml", ClassLoaderResourceAccessor(), database)
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
