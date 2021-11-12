package de.solidblocks.config

import liquibase.integration.spring.SpringLiquibase
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import javax.sql.DataSource

@Configuration
@ComponentScan(basePackages = ["de.solidblocks"])
@Import(LiquibaseAutoConfiguration::class)
open class TestConfiguration {
    // TODO figure out why liquibase autoconfiguration is broken
    @Bean
    open fun liquibase(dataSource: DataSource): SpringLiquibase? {
        val liquibase = SpringLiquibase()
        liquibase.dataSource = dataSource
        liquibase.changeLog = "classpath:/db/changelog/db.changelog-master.yaml"
        liquibase.setShouldRun(true)
        return liquibase
    }
}
