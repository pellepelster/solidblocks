package de.solidblocks.provisioner.consul.policy

import de.solidblocks.core.IInfrastructureResource

class ConsulPolicy(val id: String, val rules: String) :
        IConsulPolicyLookup,
        IInfrastructureResource<ConsulPolicy, ConsulPolicyRuntime> {

    override fun id(): String {
        return id
    }

}
