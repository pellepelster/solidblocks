package de.solidblocks.cloud.provisioner.garagefs.bucket

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.pass.PassSecret

class GarageFsBucketLookup(
    name: String,
    val server: HetznerServerLookup,
    val adminToken: PassSecret,
) : InfrastructureResourceLookup<GarageFsBucketRuntime>(name, emptySet())
