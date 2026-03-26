package de.solidblocks.cloud.provisioner.garagefs.layout

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.pass.PassSecretLookup

class GarageFsLayoutLookup(
    name: String,
    val server: HetznerServerLookup,
    val adminToken: PassSecretLookup,
) : InfrastructureResourceLookup<GarageFsLayoutRuntime>(name, emptySet())
