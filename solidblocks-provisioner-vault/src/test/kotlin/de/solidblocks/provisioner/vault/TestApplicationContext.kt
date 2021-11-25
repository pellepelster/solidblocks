package de.solidblocks.provisioner.vault

import de.solidblocks.cloud.config.CloudConfigurationContext
import de.solidblocks.cloud.config.CloudConfigurationManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.TestPropertySource
import java.util.*

@Configuration
@ComponentScan(basePackages = ["de.solidblocks"])
open class TestApplicationContext {

    @Bean
    open fun cloudConfigurationContext(cloudConfigurationManager: CloudConfigurationManager): CloudConfigurationContext {

        val cloudName = UUID.randomUUID().toString()
        val environmentName = UUID.randomUUID().toString()

        cloudConfigurationManager.createCloud(cloudName, "domain1", emptyList())
        cloudConfigurationManager.createEnvironment(cloudName, environmentName)

        return CloudConfigurationContext(
                cloudConfigurationManager.cloudByName(cloudName),
                cloudConfigurationManager.environmentByName(cloudName, environmentName))
    }

}
