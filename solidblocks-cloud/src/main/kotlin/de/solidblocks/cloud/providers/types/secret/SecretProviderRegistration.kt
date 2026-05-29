package de.solidblocks.cloud.providers.types.secret

import de.solidblocks.cloud.providers.ProviderCategory
import de.solidblocks.cloud.providers.ProviderConfigurationRuntime
import de.solidblocks.cloud.providers.ProviderManager
import de.solidblocks.cloud.providers.ProviderRegistration

interface SecretProviderRegistration<C : SecretProviderConfiguration, R : ProviderConfigurationRuntime, M : ProviderManager<C, R>> : ProviderRegistration<C, R, M> {
    override val category: ProviderCategory get() = ProviderCategory.secret
}
