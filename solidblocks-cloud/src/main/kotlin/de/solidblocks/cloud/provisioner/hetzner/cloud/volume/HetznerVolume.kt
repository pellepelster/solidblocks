package de.solidblocks.cloud.provisioner.hetzner.cloud.volume

import de.solidblocks.cloud.api.resources.BaseLabeledInfrastructureResource
import de.solidblocks.cloud.utils.ByteSize

class HetznerVolume(
    name: String,
    val location: String,
    val size: ByteSize,
    labels: Map<String, String> = emptyMap(),
    val protected: Boolean = true,
) : BaseLabeledInfrastructureResource<HetznerVolumeRuntime>(name, emptySet(), labels) {
    fun asLookup() = HetznerVolumeLookup(name)
    override fun logText() = "volume '$name'"
    override val lookupType = HetznerVolumeLookup::class
}