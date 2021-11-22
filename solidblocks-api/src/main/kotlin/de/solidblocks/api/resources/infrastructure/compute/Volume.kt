package de.solidblocks.api.resources.infrastructure.compute

import de.solidblocks.core.IInfrastructureResource

data class Volume(
        val name: String,
        val location: String,
) :
        IVolumeLookup,
        IInfrastructureResource<Volume, VolumeRuntime> {

    override fun name(): String {
        return this.name
    }
}
