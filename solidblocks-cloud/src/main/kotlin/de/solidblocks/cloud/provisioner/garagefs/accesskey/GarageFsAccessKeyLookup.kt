package de.solidblocks.cloud.provisioner.garagefs.accesskey

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.pass.PassSecret

class GarageFsAccessKeyLookup(
    name: String,
    val server: HetznerServerLookup,
    val adminToken: PassSecret,
) : InfrastructureResourceLookup<GarageFsAccessKeyRuntime>(name, emptySet())
