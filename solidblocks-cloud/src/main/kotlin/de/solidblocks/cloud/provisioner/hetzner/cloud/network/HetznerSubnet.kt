package de.solidblocks.cloud.provisioner.hetzner.cloud.network

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource

class HetznerSubnet(
    val subnet: String,
    val network: HetznerNetworkLookup,
    dependsOn: Set<BaseInfrastructureResource<*>> = emptySet(),
) : BaseInfrastructureResource<HetznerSubnetRuntime>(subnet, setOf(network) + dependsOn) {

    fun asLookup() = HetznerSubnetLookup(name, network)

    override val lookupType = HetznerSubnetLookup::class

}
