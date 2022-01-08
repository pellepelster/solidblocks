package de.solidblocks.provisioner.consul.policy

import de.solidblocks.core.IInfrastructureResource

class ConsulPolicy(override val name: String, val rules: String) :
    IConsulPolicyLookup,
    IInfrastructureResource<ConsulPolicy, ConsulPolicyRuntime>
