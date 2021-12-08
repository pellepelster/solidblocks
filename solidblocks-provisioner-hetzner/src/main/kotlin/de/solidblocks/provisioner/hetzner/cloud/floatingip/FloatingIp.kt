package de.solidblocks.provisioner.hetzner.cloud.floatingip

import de.solidblocks.core.IInfrastructureResource

data class FloatingIp(val id: String, val location: String, val labels: Map<String, String>) : IFloatingIpLookup, IInfrastructureResource<FloatingIp, FloatingIpRuntime> {

    override fun id(): String {
        return this.id
    }
}
