package de.solidblocks.cloud.provisioner.hetzner.cloud.network

import de.solidblocks.cloud.api.resources.InfrastructureResource

data class Network(
    override val name: String,
    val ipRange: String,
) : InfrastructureResource<Network, NetworkRuntime>() {
  fun asLookup() = NetworkLookup(name)
}
