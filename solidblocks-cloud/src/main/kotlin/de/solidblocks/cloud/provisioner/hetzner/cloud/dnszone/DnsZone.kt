package de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone

import de.solidblocks.cloud.api.resources.LabeledInfrastructureResource

data class DnsZone(
    override val name: String,
    override val labels: Map<String, String> = emptyMap(),
) : LabeledInfrastructureResource<DnsZoneRuntime>(labels) {
  fun asLookup() = DnsZoneLookup(name)
}
