package de.solidblocks.provisioner.consul

import com.orbitz.consul.Consul
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
@ComponentScan(basePackages = ["de.solidblocks.provisioner.consul"])
open class TestConfiguration {

    @Bean
    @Primary
    open fun consulClientWrapper(): Consul {
        return Consul.builder().withTokenAuth("master-token").build()
    }

}