package de.solidblocks.provisioner.consul.token

import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.core.IResource
import de.solidblocks.provisioner.consul.policy.ConsulPolicy
import de.solidblocks.provisioner.consul.policy.ConsulPolicyRuntime
import java.util.*

class ConsulToken(val id: UUID, val description: String, val policies: Set<ConsulPolicy>) :
    IConsulTokenLookup,
    IInfrastructureResource<ConsulToken, ConsulPolicyRuntime> {
    override fun id() = id.toString()

    override fun getParents(): Set<IResource> {
        return policies
    }
}
