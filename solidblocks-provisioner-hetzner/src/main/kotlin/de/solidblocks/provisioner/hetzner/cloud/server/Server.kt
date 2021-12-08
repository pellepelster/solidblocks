package de.solidblocks.provisioner.hetzner.cloud.server

import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.provisioner.hetzner.cloud.network.INetworkLookup
import de.solidblocks.provisioner.hetzner.cloud.ssh.ISshKeyLookup
import de.solidblocks.provisioner.hetzner.cloud.volume.IVolumeLookup

class Server(
    val id: String,
    val network: INetworkLookup,
    val userData: UserData,
    val location: String,
    val volume: IVolumeLookup? = null,
    val sshKeys: Set<ISshKeyLookup> = emptySet(),
    val dependencies: List<IInfrastructureResource<*, *>> = emptyList(),
    val labels: Map<String, String>
) :
    IServerLookup,
    IInfrastructureResource<Server, ServerRuntime> {

    override fun getParents() = listOfNotNull(userData, network, volume) + sshKeys + dependencies

    override fun id(): String {
        return this.id
    }
}
