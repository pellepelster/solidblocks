package de.solidblocks.cloud.provisioner.garagefs.layout

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.provisioner.garagefs.permission.GarageFsPermissionRuntime
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.pass.PassSecret

class GarageFsLayout(
    name: String,
    val capacity: Long,
    val server: HetznerServer,
    val adminToken: PassSecret,
) : BaseInfrastructureResource<GarageFsPermissionRuntime>(name, setOf(server)) {
    fun asLookup() = GarageFsLayoutLookup(name, server.asLookup(), adminToken)
    override fun logText() = "GarageFS access key '$name' on ${server.logText()}"
    override val lookupType = GarageFsLayoutLookup::class
}
