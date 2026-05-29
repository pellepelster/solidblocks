package de.solidblocks.cloud.providers.protonpass

import de.solidblocks.cloud.providers.types.secret.SecretProviderConfiguration

class ProtonPassProviderConfiguration(override val name: String, val vaultName: String?) : SecretProviderConfiguration {
    override val type = PROTONPASS_PROVIDER_TYPE
}
