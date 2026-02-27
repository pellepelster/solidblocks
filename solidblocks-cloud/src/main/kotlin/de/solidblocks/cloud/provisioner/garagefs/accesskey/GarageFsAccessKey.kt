package de.solidblocks.cloud.provisioner.garagefs.accesskey

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.provisioner.garagefs.permission.GarageFsPermissionRuntime
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.pass.PassSecret

class GarageFsAccessKey(
    name: String,
    val server: HetznerServer,
    val adminToken: PassSecret,
) : BaseInfrastructureResource<GarageFsPermissionRuntime>(name, setOf(server)) {
    fun asLookup() = GarageFsAccessKeyLookup(name, server.asLookup(), adminToken)
    override fun logText() = "GarageFS access key '$name' on ${server.logText()}"
    override val lookupType = GarageFsAccessKeyLookup::class
}
