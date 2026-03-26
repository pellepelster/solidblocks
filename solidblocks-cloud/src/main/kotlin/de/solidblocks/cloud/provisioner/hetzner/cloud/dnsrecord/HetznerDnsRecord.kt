package de.solidblocks.cloud.provisioner.hetzner.cloud.dnsrecord

import de.solidblocks.cloud.api.resources.BaseLabeledInfrastructureResource
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.HetznerDnsZoneLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.hetzner.cloud.resources.RRType

class HetznerDnsRecord(
    name: String,
    val zone: HetznerDnsZoneLookup,
    val values: List<HetznerServerLookup>,
    val ttl: Int = 60,
    val type: RRType = RRType.A,
    labels: Map<String, String> = emptyMap(),
) : BaseLabeledInfrastructureResource<HetznerDnsRecordRuntime>(name, setOf(zone), labels) {
  fun asLookup() = HetznerDnsRecordLookup(name, zone)

  override fun logText() = "DNS record '$name.${zone.name}/$type'"

  override val lookupType = HetznerDnsRecordLookup::class
}
