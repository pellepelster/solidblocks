package de.solidblocks.api.resources.infrastructure.compute

import de.solidblocks.core.IInfrastructureResource

data class Volume(
        val id: String,
        val location: String,
) :
        IVolumeLookup,
        IInfrastructureResource<Volume, VolumeRuntime> {

    override fun id(): String {
        return this.id
    }
}
