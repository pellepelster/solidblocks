package de.solidblocks.cloud.provisioner.hetzner.cloud.server

import de.solidblocks.cloud.api.endpoint.Endpoint
import de.solidblocks.cloud.api.resources.LabeledInfrastructureResourceRuntime
import de.solidblocks.hetzner.cloud.resources.ServerStatus

data class HetznerServerRuntime(
    val id: Long,
    val name: String,
    val status: ServerStatus,
    val image: String,
    val type: String,
    override val labels: Map<String, String>,
    val volumes: List<String>,
    val privateIpv4: String?,
    val publicIpv4: String?,
    val endpoints: List<Endpoint>,
    val sshPort: Int = 22,
) : LabeledInfrastructureResourceRuntime {
  override fun endpoints(): List<Endpoint> = endpoints
}
