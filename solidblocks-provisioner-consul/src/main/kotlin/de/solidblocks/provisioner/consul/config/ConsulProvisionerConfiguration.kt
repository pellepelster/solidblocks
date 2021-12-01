package de.solidblocks.provisioner.consul.config

import com.orbitz.consul.Consul
import de.solidblocks.cloud.config.CloudConfigurationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
open class ConsulProvisionerConfiguration {

    @Bean
    open fun consulClientWrapper(cloudConfigurationContext: CloudConfigurationContext): Consul {
        return Consul.builder().withAclToken("xxx").build()
    }

}