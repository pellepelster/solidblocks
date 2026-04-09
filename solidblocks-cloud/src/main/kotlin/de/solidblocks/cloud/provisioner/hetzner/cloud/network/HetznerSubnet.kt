package de.solidblocks.cloud.provisioner.hetzner.cloud.network

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource

class HetznerSubnet(val subnet: String, val network: HetznerNetworkLookup) : BaseInfrastructureResource<HetznerSubnetRuntime>(subnet, setOf(network)) {

    fun asLookup() = HetznerSubnetLookup(name, network)

    override fun logText() = "subnet '$subnet'"

    override val lookupType = HetznerSubnetLookup::class
}
