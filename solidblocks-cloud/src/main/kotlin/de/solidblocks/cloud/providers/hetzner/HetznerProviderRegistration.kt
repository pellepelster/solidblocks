package de.solidblocks.cloud.providers.hetzner

import de.solidblocks.cloud.providers.CloudResourceProviderRegistration

val HETZNER_PROVIDER_TYPE = "hcloud"

class HetznerProviderRegistration :
    CloudResourceProviderRegistration<
        HetznerProviderConfiguration,
        HetznerProviderRuntime,
        HetznerProviderManager,
        > {

    override val supportedConfiguration = HetznerProviderConfiguration::class
    override val supportedRuntime = HetznerProviderRuntime::class

    override fun createManager() = HetznerProviderManager()

    override fun createFactory() = HetznerProviderConfigurationFactory()

    override val type = HETZNER_PROVIDER_TYPE
}
