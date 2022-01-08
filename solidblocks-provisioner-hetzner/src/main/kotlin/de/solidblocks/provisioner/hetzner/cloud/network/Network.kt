package de.solidblocks.provisioner.hetzner.cloud.network

import de.solidblocks.core.IInfrastructureResource

data class Network(
    override val name: String,
    val ipRange: String,
    val labels: Map<String, String>
) : INetworkLookup, IInfrastructureResource<Network, NetworkRuntime>
