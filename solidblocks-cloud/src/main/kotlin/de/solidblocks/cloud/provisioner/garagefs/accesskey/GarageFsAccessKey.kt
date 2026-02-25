package de.solidblocks.cloud.provisioner.garagefs.accesskey

import de.solidblocks.cloud.api.resources.InfrastructureResource
import de.solidblocks.cloud.provisioner.garagefs.permission.GarageFsPermissionRuntime
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.pass.PassSecret

class GarageFsAccessKey(
    override val name: String,
    val server: HetznerServer,
    val adminToken: PassSecret,
) : InfrastructureResource<GarageFsPermissionRuntime>() {
    fun asLookup() = GarageFsAccessKeyLookup(name, server.asLookup(), adminToken)
    override val dependsOn = setOf(server)
    override fun logText() = "GarageFS access key '$name' on ${server.logText()}"
}
