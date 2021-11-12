package de.solidblocks.api.resources.infrastructure.compute

import de.solidblocks.core.IDataSource
import de.solidblocks.core.IInfrastructureResource

data class Volume(
    val name: String,
    val location: String,
) :
    IInfrastructureResource<VolumeRuntime> {

    override fun getParents(): List<IInfrastructureResource<*>> {
        return listOf()
    }

    override fun getParentDataSources(): List<IDataSource<*>> {
        return listOf()
    }

    override fun name(): String {
        return this.name
    }
}
