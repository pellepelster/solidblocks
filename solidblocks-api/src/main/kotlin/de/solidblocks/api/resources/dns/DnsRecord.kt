package de.solidblocks.api.resources.dns

import de.solidblocks.api.resources.infrastructure.compute.Server
import de.solidblocks.api.resources.infrastructure.network.FloatingIp
import de.solidblocks.core.IInfrastructureResource

open class DnsRecord(
        val id: String,
        val floatingIp: FloatingIp? = null,
        val server: Server? = null,
        val dnsZone: IDnsZoneLookup,
        val ttl: Int = 60
) : IDnsRecordLookup, IInfrastructureResource<DnsRecord, DnsRecordRuntime> {

    override fun getParents() = listOfNotNull(floatingIp, server)

    override fun dnsZone(): IDnsZoneLookup {
        return dnsZone
    }

    override fun id(): String {
        return this.id
    }

}
