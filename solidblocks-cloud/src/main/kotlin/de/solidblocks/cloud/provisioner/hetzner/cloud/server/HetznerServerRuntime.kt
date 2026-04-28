package de.solidblocks.cloud.provisioner.hetzner.cloud.server

import de.solidblocks.cloud.api.endpoint.Endpoint
import de.solidblocks.cloud.api.endpoint.EndpointProtocol
import de.solidblocks.cloud.api.resources.BaseLabeledInfrastructureResourceRuntime
import de.solidblocks.cloud.provisioner.context.ProvisionerContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.HetznerVolumeLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.HetznerVolumeRuntime
import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType
import de.solidblocks.hetzner.cloud.resources.ServerResponse
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
) : BaseLabeledInfrastructureResourceRuntime(labels, endpoints) {
    override fun logText() = "server '$name'"
}


suspend fun ServerResponse.toRuntime(api: HetznerApi, context: ProvisionerContext) = HetznerServerRuntime(
    this.id,
    this.name,
    this.status,
    this.image.name ?: "unknown",
    HetznerServerType.valueOf(this.type.name),
    HetznerLocation.valueOf(this.location.name),
    this.labels,
    this.volumes
        .mapNotNull { api.volumes.get(it) }
        .mapNotNull { context.lookup(HetznerVolumeLookup(it.name)) },
    this.privateNetwork.firstOrNull()?.ip,
    this.publicNetwork?.ipv4?.ip,
    this.publicNetwork?.ipv4?.ip?.let { listOf(Endpoint(this.name, it, 22, EndpointProtocol.ssh)) }
        ?: emptyList(),
)
