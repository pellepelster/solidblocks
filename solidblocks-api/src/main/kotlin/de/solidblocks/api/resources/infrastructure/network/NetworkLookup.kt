package de.solidblocks.api.resources.infrastructure.network

data class NetworkLookup(val id: String) : INetworkLookup {

    override fun id(): String {
        return this.id
    }
}
