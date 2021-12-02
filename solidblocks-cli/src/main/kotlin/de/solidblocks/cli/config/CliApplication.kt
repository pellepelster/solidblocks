package de.solidblocks.cli.config

import de.solidblocks.cloud.config.CloudConfigurationContext
import de.solidblocks.cloud.config.CloudConfigurationManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile

@SpringBootApplication(scanBasePackages = ["de.solidblocks"])
@Profile("!CloudCreate")
open class CliApplication {
    @Bean
    open fun cloudConfigurationContext(
        @Value("\${cloud.name}") cloudName: String,
        @Value("\${environment.name}") environmentName: String,
        cloudConfigurationManager: CloudConfigurationManager
    ): CloudConfigurationContext {
        val cloud = cloudConfigurationManager.cloudByName(cloudName)
        val environment = cloudConfigurationManager.environmentByName(cloudName, environmentName)
        return CloudConfigurationContext(cloud, environment)
    }

}
