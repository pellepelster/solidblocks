package de.solidblocks.cloud.providers.pass

import de.solidblocks.cloud.providers.ProviderRegistration

class PassProviderRegistration : ProviderRegistration<PassProviderConfiguration, PassProviderRuntime, PassProviderManager> {
    override val supportedConfiguration = PassProviderConfiguration::class
    override val supportedRuntime = PassProviderRuntime::class

    override fun createManager() = PassProviderManager()

    override fun createFactory() = PassProviderConfigurationFactory()

    override val type = "pass"
}
