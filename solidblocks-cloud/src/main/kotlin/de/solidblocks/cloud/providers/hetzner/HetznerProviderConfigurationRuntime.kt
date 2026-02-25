package de.solidblocks.cloud.providers.hetzner

import de.solidblocks.cloud.providers.ProviderRuntime

data class HetznerProviderConfigurationRuntime(val cloudToken: String, val defaultLocation: String, val defaultInstanceType: String) : ProviderRuntime
