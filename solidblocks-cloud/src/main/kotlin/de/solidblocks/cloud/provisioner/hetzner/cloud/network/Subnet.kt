package de.solidblocks.cloud.provisioner.hetzner.cloud.network

import de.solidblocks.cloud.api.resources.InfrastructureResource

data class Subnet(
    val subnet: String,
    val network: NetworkLookup,
    val extraDependsOn: Set<InfrastructureResource<*>> = emptySet(),
) : InfrastructureResource<SubnetRuntime>() {

  override val name = subnet

  override val dependsOn = setOf(network) + extraDependsOn

  fun asLookup() = SubnetLookup(name, network)
}
