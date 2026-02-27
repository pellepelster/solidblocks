package de.solidblocks.cloud.provisioner.hetzner.cloud.network

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource

class HetznerNetwork(name: String, val ipRange: String) :
    BaseInfrastructureResource<HetznerNetworkRuntime>(name, emptySet()) {

    fun asLookup() = HetznerNetworkLookup(name)

    override val lookupType = HetznerNetworkLookup::class

}
