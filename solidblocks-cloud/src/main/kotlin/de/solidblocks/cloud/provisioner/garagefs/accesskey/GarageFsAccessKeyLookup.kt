package de.solidblocks.cloud.provisioner.garagefs.accesskey

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.pass.PassSecret
import de.solidblocks.cloud.provisioner.secret.GenericSecret

class GarageFsAccessKeyLookup(name: String, val server: HetznerServerLookup, val adminToken: GenericSecret) : InfrastructureResourceLookup<GarageFsAccessKeyRuntime>(name, emptySet())
