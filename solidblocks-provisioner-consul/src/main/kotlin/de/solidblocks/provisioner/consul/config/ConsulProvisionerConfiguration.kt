package de.solidblocks.provisioner.consul.config

import com.ecwid.consul.v1.ConsulClient
import de.solidblocks.cloud.config.CloudConfigurationContext
import de.solidblocks.provisioner.consul.provider.ConsulClientWrapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class ConsulProvisionerConfiguration {

    @Bean
    open fun consulClientWrapper(cloudConfigurationContext: CloudConfigurationContext): ConsulClientWrapper {
        return ConsulClientWrapper(ConsulClient(), "token")
    }

}