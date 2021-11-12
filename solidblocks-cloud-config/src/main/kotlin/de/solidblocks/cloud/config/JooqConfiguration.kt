package de.solidblocks.cloud.config

import org.jooq.impl.DataSourceConnectionProvider
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.DefaultDSLContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import javax.sql.DataSource

@Configuration
open class JooqConfiguration(val dataSource: DataSource) {

    @Bean
    open fun connectionProvider(): DataSourceConnectionProvider {
        return DataSourceConnectionProvider(TransactionAwareDataSourceProxy(dataSource))
    }

    @Bean
    open fun dsl(): DefaultDSLContext {
        return DefaultDSLContext(configuration())
    }

    open fun configuration(): DefaultConfiguration {
        val jooqConfiguration = DefaultConfiguration()
        jooqConfiguration.set(connectionProvider())
        return jooqConfiguration
    }
}
