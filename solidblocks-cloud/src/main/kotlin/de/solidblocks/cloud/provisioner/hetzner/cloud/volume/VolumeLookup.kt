package de.solidblocks.cloud.provisioner.hetzner.cloud.volume

import de.solidblocks.cloud.api.resources.ResourceLookup

data class VolumeLookup(override val name: String) : ResourceLookup<VolumeRuntime>
