package de.solidblocks.provisioner.hetzner.cloud.network

data class NetworkLookup(val id: String) : INetworkLookup {

    override fun id(): String {
        return this.id
    }
}
