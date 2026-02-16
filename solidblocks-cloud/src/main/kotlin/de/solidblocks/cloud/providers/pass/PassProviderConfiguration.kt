package de.solidblocks.cloud.providers.pass

import de.solidblocks.cloud.providers.ProviderConfiguration

class PassProviderConfiguration(override val name: String) : ProviderConfiguration {
  override val type = "pass"
}
