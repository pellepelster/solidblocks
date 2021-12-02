package de.solidblocks.provisioner.vault

import de.solidblocks.cloud.config.CloudConfigurationContext
import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.cloud.config.DbConfiguration
import de.solidblocks.provisioner.Provisioner
import liquibase.integration.spring.SpringLiquibase
import org.springframework.context.annotation.*
import java.util.*
import javax.sql.DataSource

@Configuration
@ComponentScan(
    basePackages = ["de.solidblocks.provisioner.vault"],
    basePackageClasses = [Provisioner::class, CloudConfigurationManager::class]
)
@Import(DbConfiguration::class)
open class TestApplicationContext {

    @Bean
    @DependsOn("liquibase")
    open fun cloudConfigurationContext(cloudConfigurationManager: CloudConfigurationManager): CloudConfigurationContext {

        val cloudName = UUID.randomUUID().toString()
        val environmentName = UUID.randomUUID().toString()

        cloudConfigurationManager.createCloud(cloudName, "domain1", emptyList())
        cloudConfigurationManager.createEnvironment(cloudName, environmentName)

        return CloudConfigurationContext(
            cloudConfigurationManager.cloudByName(cloudName),
            cloudConfigurationManager.environmentByName(cloudName, environmentName)
        )
    }

    @Bean
    open fun liquibase(dataSource: DataSource): SpringLiquibase {

        val liquibase = SpringLiquibase()
        liquibase.dataSource = dataSource
        liquibase.changeLog = "classpath:/db/changelog/db.changelog-master.yaml"
        liquibase.setShouldRun(true)

        return liquibase
    }
}
