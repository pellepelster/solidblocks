package de.solidblocks.api.resources.infrastructure.network

import de.solidblocks.core.IInfrastructureResource

data class FloatingIp(val name: String, val location: String, val labels: Map<String, String>) : IInfrastructureResource<FloatingIpRuntime> {

    override fun name(): String {
        return this.name
    }
}
