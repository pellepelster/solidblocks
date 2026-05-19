package de.solidblocks.cloud.provisioner.hetzner.cloud.floatingip

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup

class HetznerFloatingIpLookup(name: String) : InfrastructureResourceLookup<HetznerFloatingIpRuntime>(name, emptySet()) {
    override fun logText() = "floating IP '$name'"
}
