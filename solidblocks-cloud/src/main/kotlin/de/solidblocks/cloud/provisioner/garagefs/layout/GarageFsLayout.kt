package de.solidblocks.cloud.provisioner.garagefs.layout

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.provisioner.garagefs.permission.GarageFsPermissionRuntime
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.pass.PassSecret

class GarageFsLayout(
    val capacity: Long,
    val server: HetznerServer,
    val adminToken: PassSecret,
) : BaseInfrastructureResource<GarageFsPermissionRuntime>("GarageFsLayout", setOf(server, adminToken)) {
    fun asLookup() = GarageFsLayoutLookup(name, server.asLookup(), adminToken)
    override fun logText() = "GarageFS Layout '$name' on ${server.logText()}"
    override val lookupType = GarageFsLayoutLookup::class
}
