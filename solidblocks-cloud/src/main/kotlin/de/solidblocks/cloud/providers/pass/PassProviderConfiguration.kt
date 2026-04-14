package de.solidblocks.cloud.providers.pass

import de.solidblocks.cloud.providers.types.secret.SecretProviderConfiguration

class PassProviderConfiguration(override val name: String, val passwordStoreDir: String?) : SecretProviderConfiguration {
    override val type = PASS_PROVIDER_TYPE
}
