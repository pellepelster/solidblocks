package de.solidblocks.provisioner.consul

import com.orbitz.consul.Consul
import de.solidblocks.provisioner.consul.policy.ConsulPolicyProvisioner
import de.solidblocks.provisioner.consul.token.ConsulTokenProvisioner
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
@ComponentScan(basePackageClasses = [ConsulPolicyProvisioner::class, ConsulTokenProvisioner::class])
open class TestApplicationContext {

    @Bean
    @Primary
    open fun consulClientWrapper(@Value("\${consul.addr}") consulAddr: String): Consul {
        return Consul.builder().withUrl(consulAddr).withTokenAuth("master-token").build()
    }
}
