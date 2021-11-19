package de.solidblocks.api.resources.infrastructure.compute

import de.solidblocks.api.resources.infrastructure.network.Network
import de.solidblocks.api.resources.infrastructure.ssh.SshKey
import de.solidblocks.core.IDataSource
import de.solidblocks.core.IInfrastructureResource

class Server(
        val name: String,
        val network: Network,
        val userData: UserDataDataSource,
        val location: String,
        val volume: Volume? = null,
        val sshKeys: Set<SshKey> = emptySet(),
        val dependencies: List<IInfrastructureResource<*, *>> = emptyList(),
        val labels: Map<String, String> = emptyMap()
) :
        IInfrastructureResource<Server, ServerRuntime> {

    override fun getParents(): List<IInfrastructureResource<*, *>> {
        val result = ArrayList<IInfrastructureResource<*, *>>()

        result.add(network)
        sshKeys.forEach { result.add(it) }
        result.addAll(dependencies)

        volume?.let {
            result.add(it)
        }

        return result
    }

    override fun getParentDataSources(): List<IDataSource<*>> {
        return listOf(userData)
    }

    override fun name(): String {
        return this.name
    }
}
