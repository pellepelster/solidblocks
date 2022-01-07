package de.solidblocks.provisioner.hetzner.cloud.network

import de.solidblocks.core.IInfrastructureResource

data class Subnet(val subnet: String, val network: INetworkLookup) : ISubnetLookup, IInfrastructureResource<Subnet, SubnetRuntime> {

    override fun network(): INetworkLookup {
        return network
    }

    override fun id(): String {
        return subnet
    }

    override fun getParents() = setOf(network)
}
