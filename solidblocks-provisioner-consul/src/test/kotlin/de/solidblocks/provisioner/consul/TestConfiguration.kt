package de.solidblocks.provisioner.consul

import com.ecwid.consul.v1.ConsulClient
import de.solidblocks.provisioner.consul.provider.ConsulClientWrapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
@ComponentScan(basePackages = ["de.solidblocks.provisioner.consul"])
open class TestConfiguration {

    @Bean
    @Primary
    open fun consulClientWrapper(): ConsulClientWrapper {
        return ConsulClientWrapper(ConsulClient(), "master-token")
    }

}