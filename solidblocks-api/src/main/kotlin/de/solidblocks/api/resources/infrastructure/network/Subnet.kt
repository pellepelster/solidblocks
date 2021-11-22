package de.solidblocks.api.resources.infrastructure.network

import de.solidblocks.core.IInfrastructureResource
import org.apache.commons.net.util.SubnetUtils

data class Subnet(val subnet: SubnetUtils, val network: INetworkLookup) : ISubnetLookup, IInfrastructureResource<Subnet, SubnetRuntime> {

    override fun network(): INetworkLookup {
        return network
    }

    override fun name(): String {
        return subnet.info.cidrSignature
    }
}
