package de.solidblocks.cloud.provisioner.garagefs.bucket

import de.solidblocks.cloud.api.resources.InfrastructureResource
import de.solidblocks.cloud.provisioner.garagefs.permission.GarageFsPermissionRuntime
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.pass.Secret

data class GarageFsBucket(
    override val name: String,
    val server: HetznerServer,
    val adminToken: Secret,
    val websiteAccess: Boolean = false,
) : InfrastructureResource<GarageFsBucket, GarageFsPermissionRuntime>() {

  override val dependsOn = setOf(server.asLookup())

  fun asLookup() = GarageFsBucketLookup(name, server.asLookup(), adminToken)

  override fun logText() = "S3 bucket '$name'"
}
