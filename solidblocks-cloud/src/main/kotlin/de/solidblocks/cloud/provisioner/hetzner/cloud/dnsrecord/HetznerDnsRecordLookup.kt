package de.solidblocks.cloud.provisioner.hetzner.cloud.dnsrecord

import de.solidblocks.cloud.api.resources.ResourceLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.DnsZoneLookup

data class HetznerDnsRecordLookup(override val name: String, val zone: DnsZoneLookup) :
    ResourceLookup<HetznerDnsRecordRuntime>
