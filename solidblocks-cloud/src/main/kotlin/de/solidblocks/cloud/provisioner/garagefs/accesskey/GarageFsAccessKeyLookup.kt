package de.solidblocks.cloud.provisioner.garagefs.accesskey

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.secret.GenericSecret
import de.solidblocks.cloud.provisioner.secret.GenericSecretRuntime

class GarageFsAccessKeyLookup(name: String, val server: HetznerServerLookup, val adminToken: GenericSecret<GenericSecretRuntime>) : InfrastructureResourceLookup<GarageFsAccessKeyRuntime>(name, emptySet())
