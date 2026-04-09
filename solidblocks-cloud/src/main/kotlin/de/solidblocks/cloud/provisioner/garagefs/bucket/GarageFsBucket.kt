package de.solidblocks.cloud.provisioner.garagefs.bucket

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.provisioner.garagefs.permission.GarageFsPermissionRuntime
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.pass.PassSecretLookup

class GarageFsBucket(
    name: String,
    val server: HetznerServerLookup,
    val adminToken: PassSecretLookup,
    val websiteAccess: Boolean = false,
    val websiteAccessDomains: List<String> = emptyList(),
    dependsOn: Set<BaseResource> = emptySet(),
) : BaseInfrastructureResource<GarageFsPermissionRuntime>(name, setOf(server) + dependsOn) {

    fun asLookup() = GarageFsBucketLookup(name, server, adminToken)

    override fun logText() = "GarageFS S3 bucket '$name' on ${server.logText()}"

    override val lookupType = GarageFsBucketLookup::class
}
