package de.solidblocks.cloud.providers.pass

import de.solidblocks.cloud.secret.SecretProviderConfiguration

class PassProviderConfiguration(override val name: String) : SecretProviderConfiguration {
  override val type = "pass"
}
