package de.solidblocks.cloud.config

import org.jooq.impl.DataSourceConnectionProvider
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.DefaultDSLContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import javax.sql.DataSource


@Configuration
open class DbConfiguration {

    @Bean
    open fun connectionProvider(dataSource: DataSource): DataSourceConnectionProvider {
        return DataSourceConnectionProvider(TransactionAwareDataSourceProxy(dataSource))
    }

    @Bean
    open fun dsl(configuration: DefaultConfiguration): DefaultDSLContext {
        return DefaultDSLContext(configuration)
    }

    @Bean
    open fun jooqConfiguration(connectionProvider: DataSourceConnectionProvider): DefaultConfiguration {
        val jooqConfiguration = DefaultConfiguration()
        jooqConfiguration.set(connectionProvider)
        return jooqConfiguration
    }

}
