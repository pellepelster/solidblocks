package de.solidblocks.cli.config

import de.solidblocks.cloud.model.SolidblocksDatabase
import de.solidblocks.cloud.model.repositories.CloudsRepository
import org.jooq.DSLContext
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile

@SpringBootApplication
@Import(SolidblocksDatabase::class)
@Profile("CloudCreate")
open class CliApplicationCloudCreate {
    @Bean
    open fun cloudConfigurationManager(dsl: DSLContext): CloudsRepository {
        return CloudsRepository(dsl)
    }
}
