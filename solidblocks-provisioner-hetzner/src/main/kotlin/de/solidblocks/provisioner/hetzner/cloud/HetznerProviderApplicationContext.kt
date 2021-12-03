package de.solidblocks.provisioner.hetzner.cloud

import de.solidblocks.cloud.config.CloudConfigurationContext
import de.solidblocks.cloud.config.Constants
import io.pelle.hetzner.HetznerDnsAPI
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class HetznerProviderApplicationContext {

    @Bean
    open fun hetznerCloudApi(cloudContext: CloudConfigurationContext): HetznerCloudAPI {
        return HetznerCloudAPI(cloudContext.configurationValue(Constants.ConfigKeys.HETZNER_CLOUD_API_TOKEN_RW_KEY))
    }

    @Bean
    open fun hetznerDnsApi(cloudContext: CloudConfigurationContext): HetznerDnsAPI {
        return HetznerDnsAPI(cloudContext.configurationValue(Constants.ConfigKeys.HETZNER_DNS_API_TOKEN_RW_KEY))
    }

}
