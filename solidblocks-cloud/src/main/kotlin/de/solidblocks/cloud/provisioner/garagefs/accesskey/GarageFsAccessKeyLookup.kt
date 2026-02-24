package de.solidblocks.cloud.provisioner.garagefs.accesskey

import de.solidblocks.cloud.api.resources.ResourceLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.pass.PassSecret

data class GarageFsAccessKeyLookup(
    override val name: String,
    val server: HetznerServerLookup,
    val adminToken: PassSecret,
) : ResourceLookup<GarageFsAccessKeyRuntime>
