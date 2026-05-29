package de.solidblocks.cloud.provisioner.garagefs.permission

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.garagefs.accesskey.GarageFsAccessKeyLookup
import de.solidblocks.cloud.provisioner.garagefs.bucket.GarageFsBucketLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.secret.GenericSecretLookup

class GarageFsPermissionLookup(name: String, val bucket: GarageFsBucketLookup, val accessKey: GarageFsAccessKeyLookup, val server: HetznerServerLookup, val adminToken: GenericSecretLookup) :
    InfrastructureResourceLookup<GarageFsPermissionRuntime>(name, setOf(server))
