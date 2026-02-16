package de.solidblocks.cloud.provisioner.hetzner.cloud.network

import de.solidblocks.cloud.api.resources.ResourceLookup

data class NetworkLookup(override val name: String) : ResourceLookup<NetworkRuntime>
