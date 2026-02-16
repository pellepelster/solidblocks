package de.solidblocks.cloud.provisioner.hetzner.cloud.volume

import de.solidblocks.cloud.api.resources.LabeledInfrastructureResource

class Volume(
    override val name: String,
    val location: String,
    val size: Int,
    labels: Map<String, String> = emptyMap(),
    val protected: Boolean = true,
) : LabeledInfrastructureResource<Volume, VolumeRuntime>(labels) {
  fun asLookup() = VolumeLookup(name)
}
