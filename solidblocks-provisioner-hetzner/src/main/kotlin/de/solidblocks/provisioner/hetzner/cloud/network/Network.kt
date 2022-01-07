package de.solidblocks.provisioner.hetzner.cloud.network

import de.solidblocks.core.IInfrastructureResource

data class Network(
    val id: String,
    val ipRange: String,
    val labels: Map<String, String>
) : INetworkLookup, IInfrastructureResource<Network, NetworkRuntime> {

    override fun id(): String {
        return this.id
    }
}
