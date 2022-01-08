package de.solidblocks.provisioner.consul.token

import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.provisioner.consul.policy.ConsulPolicy
import de.solidblocks.provisioner.consul.policy.ConsulPolicyRuntime
import java.util.*

class ConsulToken(val id: UUID, val description: String, val policies: Set<ConsulPolicy>) :
    IConsulTokenLookup,
    IInfrastructureResource<ConsulToken, ConsulPolicyRuntime> {

    override val name: String
        get() = id.toString()

    override val parents: Set<ConsulPolicy>
        get() = policies
}
