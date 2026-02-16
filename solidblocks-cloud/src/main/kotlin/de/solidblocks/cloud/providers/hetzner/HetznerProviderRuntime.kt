package de.solidblocks.cloud.providers.hetzner

import de.solidblocks.cloud.providers.ProviderRuntime

data class HetznerProviderRuntime(val cloudToken: String) : ProviderRuntime
