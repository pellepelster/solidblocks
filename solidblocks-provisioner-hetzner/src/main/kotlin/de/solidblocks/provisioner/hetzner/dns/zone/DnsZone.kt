package de.solidblocks.provisioner.hetzner.dns.zone

import de.solidblocks.core.IInfrastructureResource

data class DnsZone(val id: String) : IDnsZoneLookup, IInfrastructureResource<DnsZone, DnsZoneRuntime> {

    override fun id(): String {
        return this.id
    }
}
