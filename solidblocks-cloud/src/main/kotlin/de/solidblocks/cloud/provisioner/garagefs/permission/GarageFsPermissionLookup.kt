package de.solidblocks.cloud.provisioner.garagefs.permission

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.garagefs.accesskey.GarageFsAccessKeyLookup
import de.solidblocks.cloud.provisioner.garagefs.bucket.GarageFsBucketLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.pass.PassSecretLookup

class GarageFsPermissionLookup(
    name: String,
    val bucket: GarageFsBucketLookup,
    val accessKey: GarageFsAccessKeyLookup,
    val server: HetznerServerLookup,
    val adminToken: PassSecretLookup,
) : InfrastructureResourceLookup<GarageFsPermissionRuntime>(name, setOf(server))
