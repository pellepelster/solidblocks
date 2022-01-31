package de.solidblocks.cloud.model

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import liquibase.Contexts
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import mu.KotlinLogging
import org.jooq.impl.DataSourceConnectionProvider
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.DefaultDSLContext
import java.io.InputStream
import javax.sql.DataSource

class SolidblocksDatabase(jdbcUrl: String) {

    private val logger = KotlinLogging.logger {}

    val dsl: DefaultDSLContext

    val datasource: DataSource

    init {

        // TODO do not log password part of the jdbc url
        logger.info { "initializing database connection for '$jdbcUrl'" }

        val config = HikariConfig()
        config.jdbcUrl = jdbcUrl

        datasource = HikariDataSource(config)

        val ds = DataSourceConnectionProvider(datasource)
        val configuration = DefaultConfiguration()
        configuration.set(ds)

        dsl = DefaultDSLContext(configuration)
    }

    fun ensureDBSchema() {
        val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(datasource.connection))
        val liquibase = Liquibase(
            "classpath:/db/changelog/db.changelog-master.yaml",
            object : ClassLoaderResourceAccessor() {
                override fun openStream(relativeTo: String?, streamPath: String?): InputStream? {
                    return super.openStreams(relativeTo, streamPath).firstOrNull()
                }
            },
            database
        )
        liquibase.update(Contexts())
    }
}
