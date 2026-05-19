package de.solidblocks.cloud.provisioner.hetzner.cloud.floatingip

import de.solidblocks.cloud.api.resources.BaseLabeledInfrastructureResourceRuntime
import de.solidblocks.hetzner.cloud.resources.FloatingIpType

class HetznerFloatingIpRuntime(
    val id: Long,
    val name: String,
    val ip: String,
    val type: FloatingIpType,
    val assigneeId: Long?,
    labels: Map<String, String>,
    val deleteProtected: Boolean,
) : BaseLabeledInfrastructureResourceRuntime(labels)
