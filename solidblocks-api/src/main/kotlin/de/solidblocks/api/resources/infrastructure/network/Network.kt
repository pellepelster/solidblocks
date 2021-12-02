package de.solidblocks.api.resources.infrastructure.network

import de.solidblocks.core.IInfrastructureResource
import org.apache.commons.net.util.SubnetUtils

data class Network(val id: String, val ipRange: SubnetUtils) : INetworkLookup, IInfrastructureResource<Network, NetworkRuntime> {

    override fun id(): String {
        return this.id
    }
}
