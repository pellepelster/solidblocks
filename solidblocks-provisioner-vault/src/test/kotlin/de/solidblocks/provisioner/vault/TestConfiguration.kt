package de.solidblocks.provisioner.vault

import liquibase.integration.spring.SpringLiquibase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
@ComponentScan(basePackages = ["de.solidblocks"])
open class TestConfiguration {
    @Bean
    open fun liquibase(dataSource: DataSource): SpringLiquibase? {
        val liquibase = SpringLiquibase()
        liquibase.dataSource = dataSource
        liquibase.changeLog = "classpath:/db/changelog/db.changelog-master.yaml"
        liquibase.setShouldRun(true)
        return liquibase
    }
}
