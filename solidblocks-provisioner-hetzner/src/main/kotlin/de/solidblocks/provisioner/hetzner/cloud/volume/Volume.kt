package de.solidblocks.provisioner.hetzner.cloud.volume

import de.solidblocks.core.IInfrastructureResource

data class Volume(
    val id: String,
    val location: String,
    val labels: Map<String, String>
) :
    IVolumeLookup,
    IInfrastructureResource<Volume, VolumeRuntime> {

    override fun id(): String {
        return this.id
    }
}
