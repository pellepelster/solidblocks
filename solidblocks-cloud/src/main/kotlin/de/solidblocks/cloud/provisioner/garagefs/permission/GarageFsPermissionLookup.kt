package de.solidblocks.cloud.provisioner.garagefs.permission

import de.solidblocks.cloud.api.resources.ResourceLookup
import de.solidblocks.cloud.provisioner.garagefs.accesskey.GarageFsAccessKeyLookup
import de.solidblocks.cloud.provisioner.garagefs.bucket.GarageFsBucketLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.pass.SecretLookup

data class GarageFsPermissionLookup(
    override val name: String,
    val bucket: GarageFsBucketLookup,
    val accessKey: GarageFsAccessKeyLookup,
    val server: HetznerServerLookup,
    val adminToken: SecretLookup,
) : ResourceLookup<GarageFsPermissionRuntime>
