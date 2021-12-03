package de.solidblocks.api.resources.infrastructure.network

import de.solidblocks.core.IInfrastructureResource

data class Network(val id: String, val ipRange: String) : INetworkLookup, IInfrastructureResource<Network, NetworkRuntime> {

    override fun id(): String {
        return this.id
    }
}
