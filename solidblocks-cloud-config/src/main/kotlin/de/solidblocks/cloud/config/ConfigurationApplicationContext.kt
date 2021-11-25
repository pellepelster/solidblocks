package de.solidblocks.cloud.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!CloudCreate")
open class ConfigurationApplicationContext {

    @Bean
    @Profile("!CloudCreate")
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