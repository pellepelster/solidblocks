package de.solidblocks.cloud.providers.hetzner

import de.solidblocks.cloud.providers.CloudResourceProviderConfiguration
import de.solidblocks.cloud.providers.ProviderConfiguration
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType

class HetznerProviderConfiguration(override val name: String, val defaultLocation: HetznerLocation, val defaultInstanceType: HetznerServerType) : CloudResourceProviderConfiguration {
    override val type = HETZNER_PROVIDER_TYPE
}

// TODO clean up when multi provider support is needed
fun List<ProviderConfiguration>.hetznerProviderConfig() = this.filterIsInstance<HetznerProviderConfiguration>().single()
