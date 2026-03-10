package de.solidblocks.cloud.provisioner.hetzner.cloud.network

import de.solidblocks.cloud.api.resources.BaseLabeledInfrastructureResource

class HetznerNetwork(name: String, val ipRange: String, labels: Map<String, String> = emptyMap(), val protected: Boolean = true) :
    BaseLabeledInfrastructureResource<HetznerNetworkRuntime>(name, emptySet(), labels) {

    fun asLookup() = HetznerNetworkLookup(name)

    override fun logText() = "network '$name'"

    override val lookupType = HetznerNetworkLookup::class
}
