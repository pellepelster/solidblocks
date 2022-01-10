package de.solidblocks.provisioner.hetzner.cloud.server

import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.provisioner.hetzner.cloud.network.INetworkLookup
import de.solidblocks.provisioner.hetzner.cloud.network.ISubnetLookup
import de.solidblocks.provisioner.hetzner.cloud.ssh.ISshKeyLookup
import de.solidblocks.provisioner.hetzner.cloud.volume.IVolumeLookup

class Server(
    override val name: String,
    // subnet is needed as dependency to ensure the network is ready, otherwise we will run into 'network $id has no free IP available'
    val subnet: ISubnetLookup?,
    val network: INetworkLookup,
    val userData: UserData,
    val location: String,
    val volume: IVolumeLookup? = null,
    val sshKeys: Set<ISshKeyLookup> = emptySet(),
    dependencies: Set<IInfrastructureResource<*, *>> = emptySet(),
    val labels: Map<String, String>
) :
    IServerLookup,
    IInfrastructureResource<Server, ServerRuntime> {

    override val parents = setOfNotNull(userData, network, subnet, volume) + sshKeys + dependencies
}
