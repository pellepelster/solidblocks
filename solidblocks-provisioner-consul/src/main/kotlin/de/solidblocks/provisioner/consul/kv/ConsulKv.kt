package de.solidblocks.provisioner.consul.kv

import de.solidblocks.core.IInfrastructureResource

class ConsulKv(val key: String) :
    IConsulKvLookup,
    IInfrastructureResource<ConsulKv, ConsulKvRuntime> {

    override fun id(): String {
        return key
    }
}
