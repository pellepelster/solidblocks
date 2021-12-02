package de.solidblocks.api.resources.infrastructure.compute

import de.solidblocks.api.resources.infrastructure.network.INetworkLookup
import de.solidblocks.api.resources.infrastructure.ssh.ISshKeyLookup
import de.solidblocks.core.IInfrastructureResource

class Server(
    val id: String,
    val network: INetworkLookup,
    val userData: UserDataDataSource,
    val location: String,
    val volume: IVolumeLookup? = null,
    val sshKeys: Set<ISshKeyLookup> = emptySet(),
    val dependencies: List<IInfrastructureResource<*, *>> = emptyList(),
    val labels: Map<String, String> = emptyMap()
) :
    IServerLookup,
    IInfrastructureResource<Server, ServerRuntime> {

    override fun getParents() = listOfNotNull(userData, network, volume) + sshKeys + dependencies

    override fun id(): String {
        return this.id
    }
}
