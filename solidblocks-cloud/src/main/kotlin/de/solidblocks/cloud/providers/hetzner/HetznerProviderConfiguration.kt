package de.solidblocks.cloud.providers.hetzner

import de.solidblocks.cloud.providers.CloudResourceProviderConfiguration

class HetznerProviderConfiguration(override val name: String, val defaultLocation: String, val defaultInstanceType: String) : CloudResourceProviderConfiguration {
    override val type = HETZNER_PROVIDER_TYPE
}
