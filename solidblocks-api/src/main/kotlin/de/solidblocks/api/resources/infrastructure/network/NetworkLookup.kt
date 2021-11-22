package de.solidblocks.api.resources.infrastructure.network

data class NetworkLookup(val name: String) : INetworkLookup {

    override fun name(): String {
        return this.name
    }
}
