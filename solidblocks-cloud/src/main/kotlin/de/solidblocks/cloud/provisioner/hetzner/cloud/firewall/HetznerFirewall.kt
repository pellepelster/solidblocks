package de.solidblocks.cloud.provisioner.hetzner.cloud.firewall

import de.solidblocks.cloud.api.resources.BaseLabeledInfrastructureResource
import de.solidblocks.hetzner.cloud.resources.HetznerFirewallRule

class HetznerFirewall(name: String, val rules: List<HetznerFirewallRule>, labels: Map<String, String> = emptyMap(), val appliedToLabels: Map<String, String>) :
    BaseLabeledInfrastructureResource<HetznerFirewallRuntime>(name, emptySet(), labels) {

    fun asLookup() = HetznerFirewallLookup(name)

    override fun logText() = "firewall '$name'"

    override val lookupType = HetznerFirewallLookup::class
}
