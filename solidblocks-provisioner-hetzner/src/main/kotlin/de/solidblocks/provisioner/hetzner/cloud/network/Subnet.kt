package de.solidblocks.provisioner.hetzner.cloud.network

import de.solidblocks.core.IInfrastructureResource

data class Subnet(val subnet: String, override val network: INetworkLookup) :
    ISubnetLookup,
    IInfrastructureResource<Subnet, SubnetRuntime> {

    override val name = subnet

    override val parents = setOf(network)
}
