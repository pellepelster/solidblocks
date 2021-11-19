package de.solidblocks.api.resources.infrastructure.network

import de.solidblocks.core.IInfrastructureResource
import org.apache.commons.net.util.SubnetUtils

data class Network(val name: String, val subnet: SubnetUtils.SubnetInfo) : IInfrastructureResource<Network, NetworkRuntime> {

    override fun name(): String {
        return this.name
    }
}
