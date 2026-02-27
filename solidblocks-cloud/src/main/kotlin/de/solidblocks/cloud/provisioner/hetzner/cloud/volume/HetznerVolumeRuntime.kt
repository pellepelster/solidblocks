package de.solidblocks.cloud.provisioner.hetzner.cloud.volume

import de.solidblocks.cloud.api.resources.BaseLabeledInfrastructureResourceRuntime

class HetznerVolumeRuntime(
    val id: Long,
    val name: String,
    val device: String,
    val server: Long?,
    labels: Map<String, String>,
    val deleteProtected: Boolean,
) : BaseLabeledInfrastructureResourceRuntime(labels)
