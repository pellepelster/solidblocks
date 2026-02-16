package de.solidblocks.cloud.provisioner.hetzner.cloud.server

import de.solidblocks.cloud.api.resources.ResourceLookup

data class HetznerServerLookup(override val name: String) : ResourceLookup<HetznerServerRuntime>
