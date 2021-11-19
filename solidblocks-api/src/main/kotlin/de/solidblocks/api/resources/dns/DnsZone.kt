package de.solidblocks.api.resources.dns

import de.solidblocks.core.IInfrastructureResource

data class DnsZone(val name: String) : IInfrastructureResource<DnsZone, DnsZoneRuntime> {

    override fun name(): String {
        return this.name
    }

}
