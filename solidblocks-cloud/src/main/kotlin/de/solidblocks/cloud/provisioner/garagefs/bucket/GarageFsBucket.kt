package de.solidblocks.cloud.provisioner.garagefs.bucket

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.provisioner.garagefs.permission.GarageFsPermissionRuntime
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.pass.PassSecret

class GarageFsBucket(
    name: String,
    val server: HetznerServer,
    val adminToken: PassSecret,
    val websiteAccess: Boolean = false,
    val websiteAccessDomains: List<String> = emptyList(),
) : BaseInfrastructureResource<GarageFsPermissionRuntime>(name, setOf(server)) {

    fun asLookup() = GarageFsBucketLookup(name, server.asLookup(), adminToken)

    override fun logText() = "GarageFS S3 bucket '$name' on ${server.logText()}"

    override val lookupType = GarageFsBucketLookup::class

}
