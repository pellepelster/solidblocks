package de.solidblocks.provisioner.hetzner.dns.record

import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.provisioner.hetzner.cloud.floatingip.FloatingIp
import de.solidblocks.provisioner.hetzner.cloud.server.Server
import de.solidblocks.provisioner.hetzner.dns.zone.IDnsZoneLookup

open class DnsRecord(
    override val name: String,
    val floatingIp: FloatingIp? = null,
    val server: Server? = null,
    val dnsZone: IDnsZoneLookup,
    val ttl: Int = 60
) : IDnsRecordLookup, IInfrastructureResource<DnsRecord, DnsRecordRuntime> {

    override val parents = setOfNotNull(floatingIp, server)

    override fun dnsZone(): IDnsZoneLookup {
        return dnsZone
    }
}
