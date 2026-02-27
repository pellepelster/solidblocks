package de.solidblocks.cloud.provisioner.hetzner.cloud.server

import de.solidblocks.cloud.api.endpoint.Endpoint
import de.solidblocks.cloud.api.resources.BaseLabeledInfrastructureResourceRuntime
import de.solidblocks.hetzner.cloud.resources.ServerStatus

class HetznerServerRuntime(
    val id: Long,
    val name: String,
    val status: ServerStatus,
    val image: String,
    val type: String,
    val location: String,
    labels: Map<String, String>,
    val volumes: List<String>,
    val privateIpv4: String?,
    val publicIpv4: String?,
    endpoints: List<Endpoint>,
    val sshPort: Int = 22,
) : BaseLabeledInfrastructureResourceRuntime(labels, endpoints) {
}
