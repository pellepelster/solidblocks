package de.solidblocks.cloud.provisioner.garagefs.bucket

import de.solidblocks.cloud.api.resources.ResourceLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.pass.PassSecret

data class GarageFsBucketLookup(
    override val name: String,
    val server: HetznerServerLookup,
    val adminToken: PassSecret,
) : ResourceLookup<GarageFsBucketRuntime>
