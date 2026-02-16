package de.solidblocks.cloud.provisioner.hetzner.cloud.volume

import de.solidblocks.cloud.api.resources.LabeledInfrastructureResourceRuntime

data class VolumeRuntime(
    val id: Long,
    val name: String,
    val device: String,
    val server: Long?,
    override val labels: Map<String, String>,
    val deleteProtected: Boolean,
) : LabeledInfrastructureResourceRuntime
