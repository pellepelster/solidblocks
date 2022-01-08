package de.solidblocks.provisioner.hetzner.dns.record

import de.solidblocks.core.IResourceLookup
import de.solidblocks.provisioner.hetzner.dns.zone.IDnsZoneLookup

interface IDnsRecordLookup : IResourceLookup<DnsRecordRuntime> {
    val dnsZone: IDnsZoneLookup
}
