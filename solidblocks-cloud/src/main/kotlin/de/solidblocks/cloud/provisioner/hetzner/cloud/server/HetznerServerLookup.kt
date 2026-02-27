package de.solidblocks.cloud.provisioner.hetzner.cloud.server

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup

class HetznerServerLookup(name: String) : InfrastructureResourceLookup<HetznerServerRuntime>(name, emptySet())
