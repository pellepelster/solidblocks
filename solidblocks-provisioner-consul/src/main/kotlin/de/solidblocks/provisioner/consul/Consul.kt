package de.solidblocks.provisioner.consul

import com.orbitz.consul.Consul
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.base.ProvisionerRegistry
import de.solidblocks.cloud.model.ModelConstants.CONSUL_MASTER_TOKEN_KEY
import de.solidblocks.cloud.model.model.EnvironmentModel
import de.solidblocks.provisioner.consul.kv.ConsulKvProvisioner
import de.solidblocks.provisioner.consul.policy.ConsulPolicyProvisioner
import de.solidblocks.provisioner.consul.token.ConsulTokenProvisioner

object Consul {

    // TODO use https and cloud pki for connections
    fun consulAddress(environment: EnvironmentModel) =
        "http://consul.${environment.name}.${environment.cloud.rootDomain}:8500"

    fun consulClient(environmentConfiguration: EnvironmentModel): Consul {
        return Consul.builder()
            .withUrl(consulAddress(environmentConfiguration))
            .withAclToken(environmentConfiguration.getConfigValue(CONSUL_MASTER_TOKEN_KEY))
            .withPing(false).build()
    }

    fun registerProvisioners(
        provisionerRegistry: ProvisionerRegistry,
        consul: Consul,
    ) {

        provisionerRegistry.addProvisioner(
            ConsulTokenProvisioner(consul) as IInfrastructureResourceProvisioner<Any, Any>
        )

        provisionerRegistry.addProvisioner(
            ConsulPolicyProvisioner(consul) as IInfrastructureResourceProvisioner<Any, Any>
        )

        provisionerRegistry.addProvisioner(
            ConsulKvProvisioner(consul) as IInfrastructureResourceProvisioner<Any, Any>
        )

        provisionerRegistry.addProvisioner(
            ConsulTokenProvisioner(consul) as IInfrastructureResourceProvisioner<Any, Any>
        )
    }
}
