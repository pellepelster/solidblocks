package de.solidblocks.api.resources.dns

import de.solidblocks.core.IDataSource
import de.solidblocks.core.IInfrastructureResource

data class DnsZone(val name: String) : IInfrastructureResource<DnsZoneRuntime> {

    override fun getParents(): List<IInfrastructureResource<*>> {
        return emptyList()
    }

    override fun name(): String {
        return this.name
    }

    override fun getParentDataSources(): List<IDataSource<*>> {
        return emptyList()
    }
}
