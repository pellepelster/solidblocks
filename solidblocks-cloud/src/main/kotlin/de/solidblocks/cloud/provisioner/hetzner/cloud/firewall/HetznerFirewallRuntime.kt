package de.solidblocks.cloud.provisioner.hetzner.cloud.firewall

import de.solidblocks.cloud.api.resources.BaseLabeledInfrastructureResourceRuntime
import de.solidblocks.hetzner.cloud.resources.HetznerFirewallRule

class HetznerFirewallRuntime(val id: Long, val name: String, val rules: List<HetznerFirewallRule>, labels: Map<String, String>, val appliedToLabels: Map<String, String>) :
    BaseLabeledInfrastructureResourceRuntime(labels)
