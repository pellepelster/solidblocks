package de.solidblocks.cli.config

import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.cloud.config.DbConfiguration
import org.jooq.DSLContext
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile

@SpringBootApplication
@Import(DbConfiguration::class)
@Profile("CloudCreate")
open class CliApplicationCloudCreate {
    @Bean
    open fun cloudConfigurationManager(dsl: DSLContext): CloudConfigurationManager {
        return CloudConfigurationManager(dsl)
    }
}
