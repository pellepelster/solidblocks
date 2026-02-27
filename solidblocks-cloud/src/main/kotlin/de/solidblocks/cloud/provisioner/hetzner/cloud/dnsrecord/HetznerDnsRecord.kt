package de.solidblocks.cloud.provisioner.hetzner.cloud.dnsrecord

import de.solidblocks.cloud.api.resources.BaseLabeledInfrastructureResource
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.HetznerDnsZoneLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup

class HetznerDnsRecord(
    name: String,
    val zone: HetznerDnsZoneLookup,
    val values: List<HetznerServerLookup>,
    labels: Map<String, String> = emptyMap(),
) : BaseLabeledInfrastructureResource<HetznerDnsRecordRuntime>(name, setOf(zone), labels) {
    fun asLookup() = HetznerDnsRecordLookup(name, zone)

    override fun logText() = "DNS record '$name.${zone.name}'"

    override val lookupType = HetznerDnsRecordLookup::class

}
