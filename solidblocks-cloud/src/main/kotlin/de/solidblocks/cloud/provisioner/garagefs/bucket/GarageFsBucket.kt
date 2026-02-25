package de.solidblocks.cloud.provisioner.garagefs.bucket

import de.solidblocks.cloud.api.resources.InfrastructureResource
import de.solidblocks.cloud.provisioner.garagefs.permission.GarageFsPermissionRuntime
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.pass.PassSecret

data class GarageFsBucket(
    override val name: String,
    val server: HetznerServer,
    val adminToken: PassSecret,
    val websiteAccess: Boolean = false,
    val websiteAccessDomains: List<String> = emptyList(),
) : InfrastructureResource<GarageFsBucket, GarageFsPermissionRuntime>() {

  override val dependsOn = setOf(server.asLookup())

  fun asLookup() = GarageFsBucketLookup(name, server.asLookup(), adminToken)

  override fun logText() = "GarageFS S3 bucket '$name' on ${server.logText()}"
}
