package de.solidblocks.api.resources.dns

import de.solidblocks.core.IInfrastructureResource

data class DnsZone(val id: String) : IDnsZoneLookup, IInfrastructureResource<DnsZone, DnsZoneRuntime> {

    override fun id(): String {
        return this.id
    }

}
