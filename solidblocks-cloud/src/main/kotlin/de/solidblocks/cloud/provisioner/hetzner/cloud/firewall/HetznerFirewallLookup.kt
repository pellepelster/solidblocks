package de.solidblocks.cloud.provisioner.hetzner.cloud.firewall

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup

class HetznerFirewallLookup(name: String) : InfrastructureResourceLookup<HetznerFirewallRuntime>(name, emptySet()) {
    override fun logText() = "firewall '$name'"
}
