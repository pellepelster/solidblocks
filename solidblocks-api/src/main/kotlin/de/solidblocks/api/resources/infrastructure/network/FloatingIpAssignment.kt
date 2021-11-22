package de.solidblocks.api.resources.infrastructure.network

import de.solidblocks.api.resources.infrastructure.compute.IServerLookup
import de.solidblocks.core.IInfrastructureResource

data class FloatingIpAssignment(val server: IServerLookup, val floatingIp: IFloatingIpLookup) :
        IFloatingIpAssignmentLookup,
        IInfrastructureResource<FloatingIpAssignment, FloatingIpAssignmentRuntime> {

    override fun getParents() = listOf(server, floatingIp)

    override fun floatingIp(): IFloatingIpLookup {
        return floatingIp
    }

    override fun name(): String {
        return "${this.server.name()}-${this.floatingIp.name()}"
    }
}
