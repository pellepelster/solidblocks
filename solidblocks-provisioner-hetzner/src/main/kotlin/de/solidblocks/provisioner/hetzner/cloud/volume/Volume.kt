package de.solidblocks.provisioner.hetzner.cloud.volume

import de.solidblocks.core.IInfrastructureResource

data class Volume(
    override val name: String,
    val location: String,
    val labels: Map<String, String>
) :
    IVolumeLookup,
    IInfrastructureResource<Volume, VolumeRuntime>
