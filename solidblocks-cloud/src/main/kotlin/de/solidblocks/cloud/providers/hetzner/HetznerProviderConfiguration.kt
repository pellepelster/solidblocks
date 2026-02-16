package de.solidblocks.cloud.providers.hetzner

import de.solidblocks.cloud.providers.CloudResourceProviderConfiguration

class HetznerProviderConfiguration(override val name: String) : CloudResourceProviderConfiguration {
  override val type = HETZNER_PROVIDER_TYPE
}
