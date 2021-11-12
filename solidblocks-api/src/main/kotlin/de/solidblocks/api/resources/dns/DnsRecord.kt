package de.solidblocks.api.resources.dns

import de.solidblocks.api.resources.infrastructure.compute.Server
import de.solidblocks.api.resources.infrastructure.network.FloatingIp
import de.solidblocks.core.IInfrastructureResource

data class DnsRecord(
    val name: String,
    val floatingIp: FloatingIp? = null,
    val server: Server? = null,
    val zone: DnsZone,
    val ttl: Int = 60
) : IInfrastructureResource<DnsRecordRuntime> {

    override fun getParents(): List<IInfrastructureResource<*>> {
        return listOfNotNull(floatingIp, server)
    }

    override fun name(): String {
        return this.name
    }
}
