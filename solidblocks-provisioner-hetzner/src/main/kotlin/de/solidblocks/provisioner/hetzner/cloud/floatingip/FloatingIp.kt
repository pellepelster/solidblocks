package de.solidblocks.provisioner.hetzner.cloud.floatingip

import de.solidblocks.core.IInfrastructureResource

data class FloatingIp(override val name: String, val location: String, val labels: Map<String, String>) :
    IFloatingIpLookup, IInfrastructureResource<FloatingIp, FloatingIpRuntime>
