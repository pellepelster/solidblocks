package de.solidblocks.api.resources.infrastructure.network

import de.solidblocks.core.IResourceLookup
import de.solidblocks.core.IInfrastructureResource
import org.apache.commons.net.util.SubnetUtils

data class Network(val name: String, val ipRange: SubnetUtils) : INetworkLookup, IInfrastructureResource<Network, NetworkRuntime> {

    override fun name(): String {
        return this.name
    }
}
