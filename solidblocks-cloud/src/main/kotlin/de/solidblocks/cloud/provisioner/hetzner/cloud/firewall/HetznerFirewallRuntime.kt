package de.solidblocks.cloud.provisioner.hetzner.cloud.firewall

import de.solidblocks.cloud.api.resources.BaseLabeledInfrastructureResourceRuntime
import de.solidblocks.hetzner.cloud.resources.FirewallRule

class HetznerFirewallRuntime(val id: Long, val name: String, val rules: List<FirewallRule>, labels: Map<String, String>) :
    BaseLabeledInfrastructureResourceRuntime(labels)
