package de.solidblocks.provisioner.consul.kv

import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.core.IResource

class ConsulKv(val key: String, val dependsOn: Set<IResource> = emptySet()) :
    IConsulKvLookup,
    IInfrastructureResource<ConsulKv, ConsulKvRuntime> {

    override fun getParents(): Set<IResource> {
        return dependsOn
    }

    override fun id(): String {
        return key
    }
}
