package de.solidblocks.provisioner.consul.kv

import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.core.IResource

class ConsulKv(override val name: String, override val parents: Set<IResource> = emptySet()) :
    IConsulKvLookup,
    IInfrastructureResource<ConsulKv, ConsulKvRuntime>
