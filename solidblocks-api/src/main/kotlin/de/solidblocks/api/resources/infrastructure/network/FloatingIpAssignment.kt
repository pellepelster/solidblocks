package de.solidblocks.api.resources.infrastructure.network

import de.solidblocks.api.resources.infrastructure.compute.Server
import de.solidblocks.core.IInfrastructureResource

data class FloatingIpAssignment(val server: Server, val floatingIp: FloatingIp) :
        IInfrastructureResource<FloatingIpAssignment, FloatingIpAssignmentRuntime> {

    override fun getParents(): List<IInfrastructureResource<*, *>> {
        return listOf(server as IInfrastructureResource<*, *>, floatingIp as IInfrastructureResource<*, *>)
    }

    override fun name(): String {
        return "${this.server.name}-${this.floatingIp.name}"
    }
}
