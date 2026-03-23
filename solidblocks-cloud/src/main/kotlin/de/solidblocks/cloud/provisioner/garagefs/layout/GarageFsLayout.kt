package de.solidblocks.cloud.provisioner.garagefs.layout

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.provisioner.garagefs.permission.GarageFsPermissionRuntime
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.pass.PassSecret
import de.solidblocks.cloud.provisioner.pass.PassSecretLookup

class GarageFsLayout(
    val capacity: Long,
    val server: HetznerServerLookup,
    val adminToken: PassSecretLookup,
) : BaseInfrastructureResource<GarageFsPermissionRuntime>("GarageFsLayout", setOf(server, adminToken)) {
    fun asLookup() = GarageFsLayoutLookup(name, server, adminToken)
    override fun logText() = "GarageFS Layout '$name' on ${server.logText()}"
    override val lookupType = GarageFsLayoutLookup::class
}
