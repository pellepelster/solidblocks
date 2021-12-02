package de.solidblocks.cloud.config

import liquibase.integration.spring.SpringLiquibase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import javax.sql.DataSource

@Configuration
@ComponentScan(basePackageClasses = [CloudConfigurationManager::class])
@Import(DbConfiguration::class)
open class TestApplicationContext {
    @Bean
    open fun liquibase(dataSource: DataSource): SpringLiquibase {

        val liquibase = SpringLiquibase()
        liquibase.dataSource = dataSource
        liquibase.changeLog = "classpath:/db/changelog/db.changelog-master.yaml"
        liquibase.setShouldRun(true)

        return liquibase
    }
}
