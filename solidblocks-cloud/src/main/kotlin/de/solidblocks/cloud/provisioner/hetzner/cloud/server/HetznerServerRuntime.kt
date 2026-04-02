package de.solidblocks.cloud.provisioner.hetzner.cloud.server

import de.solidblocks.cloud.api.endpoint.Endpoint
import de.solidblocks.cloud.api.resources.BaseLabeledInfrastructureResourceRuntime
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.HetznerVolumeRuntime
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType
import de.solidblocks.hetzner.cloud.resources.ServerStatus

class HetznerServerRuntime(
    val id: Long,
    val name: String,
    val status: ServerStatus,
    val image: String,
    val type: HetznerServerType,
    val location: HetznerLocation,
    labels: Map<String, String>,
    val volumes: List<HetznerVolumeRuntime>,
    val privateIpv4: String?,
    val publicIpv4: String?,
    endpoints: List<Endpoint>,
    val sshPort: Int = 22,
) : BaseLabeledInfrastructureResourceRuntime(labels, endpoints)
