package de.solidblocks.cloud.provisioner.garagefs.accesskey

import de.solidblocks.cloud.api.resources.ResourceLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.pass.Secret

data class GarageFsAccessKeyLookup(
    override val name: String,
    val server: HetznerServerLookup,
    val adminToken: Secret,
) : ResourceLookup<GarageFsAccessKeyRuntime>
