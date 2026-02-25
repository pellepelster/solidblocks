package de.solidblocks.cloud.provisioner.hetzner.cloud.dnsrecord

import de.solidblocks.cloud.api.resources.LabeledInfrastructureResource
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.DnsZoneLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup

data class HetznerDnsRecord(
    override val name: String,
    val zone: DnsZoneLookup,
    val values: List<HetznerServerLookup>,
    override val labels: Map<String, String> = emptyMap(),
) : LabeledInfrastructureResource<HetznerDnsRecordRuntime>(labels) {
  fun asLookup() = HetznerDnsRecordLookup(name, zone)

  override fun logText() = "DNS record '$name.${zone.name}'"
}
