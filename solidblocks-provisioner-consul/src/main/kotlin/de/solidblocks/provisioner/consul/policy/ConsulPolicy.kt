package de.solidblocks.provisioner.consul.policy

import de.solidblocks.core.IInfrastructureResource

class ConsulPolicy(val name: String, val rules: String) :
        IConsulPolicyLookup,
        IInfrastructureResource<ConsulPolicy, ConsulPolicyRuntime> {

    override fun name(): String {
        return name
    }

}
