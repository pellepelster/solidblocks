package de.solidblocks.cloud.providers.hetzner

import de.solidblocks.cloud.providers.ProviderConfigurtionRuntime
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType

data class HetznerProviderRuntime(
    val cloudToken: String,
    val defaultLocation1: HetznerLocation,
    val defaultInstanceType: HetznerServerType,
) : ProviderConfigurtionRuntime
