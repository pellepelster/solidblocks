package de.solidblocks.cloud.provisioner.hetzner.cloud.volume

import de.solidblocks.cloud.api.resources.BaseLabeledInfrastructureResource

class HetznerVolume(
    name: String,
    val location: String,
    val size: Int,
    labels: Map<String, String> = emptyMap(),
    val protected: Boolean = true,
) : BaseLabeledInfrastructureResource<HetznerVolumeRuntime>(name, emptySet(), labels) {
    fun asLookup() = HetznerVolumeLookup(name)
    override val lookupType = HetznerVolumeLookup::class
}
