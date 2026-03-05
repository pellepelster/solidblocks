package de.solidblocks.cloud.providers.hetzner

import de.solidblocks.cloud.providers.ProviderRuntime
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType

data class HetznerProviderConfigurationRuntime(val cloudToken: String, val defaultLocation: HetznerLocation, val defaultInstanceType: HetznerServerType) : ProviderRuntime
