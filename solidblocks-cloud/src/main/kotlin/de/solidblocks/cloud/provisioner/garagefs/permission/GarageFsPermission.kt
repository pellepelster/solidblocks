package de.solidblocks.cloud.provisioner.garagefs.permission

import de.solidblocks.cloud.api.resources.InfrastructureResource
import de.solidblocks.cloud.provisioner.garagefs.accesskey.GarageFsAccessKey
import de.solidblocks.cloud.provisioner.garagefs.bucket.GarageFsBucket
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.pass.PassSecret

data class GarageFsPermission(
    val bucket: GarageFsBucket,
    val accessKey: GarageFsAccessKey,
    val server: HetznerServer,
    val adminToken: PassSecret,
    val owner: Boolean,
    val read: Boolean,
    val write: Boolean,
) : InfrastructureResource<GarageFsPermission, GarageFsPermissionRuntime>() {

    fun asLookup() =
        GarageFsPermissionLookup(
            name,
            bucket.asLookup(),
            accessKey.asLookup(),
            server.asLookup(),
            adminToken.asLookup(),
        )

    override val name = "${bucket.name}-${accessKey.name}"

    override fun logText() = "GarageFS permission '$name' on ${server.logText()}"

}
