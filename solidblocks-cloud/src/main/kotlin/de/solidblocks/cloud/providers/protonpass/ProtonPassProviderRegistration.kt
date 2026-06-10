package de.solidblocks.cloud.providers.protonpass

import de.solidblocks.cloud.providers.types.secret.SecretProviderRegistration

val PROTONPASS_PROVIDER_TYPE = "protonpass"

class ProtonPassProviderRegistration : SecretProviderRegistration<ProtonPassProviderConfiguration, ProtonPassProviderRuntime, ProtonPassProviderManager> {
    override val supportedConfiguration = ProtonPassProviderConfiguration::class
    override val supportedRuntime = ProtonPassProviderRuntime::class

    override fun createManager() = ProtonPassProviderManager()

    override fun createFactory() = ProtonPassProviderConfigurationFactory()

    override val type = PROTONPASS_PROVIDER_TYPE
}
