package de.solidblocks.cloud.providers.hetzner

import de.solidblocks.cloud.providers.CloudResourceProviderRegistration

val HETZNER_PROVIDER_TYPE = "hcloud"

class HetznerProviderRegistration :
    CloudResourceProviderRegistration<
        HetznerProviderConfiguration,
        HetznerProviderConfigurationRuntime,
        HetznerProviderConfigurationManager,
    > {

  override val supportedConfiguration = HetznerProviderConfiguration::class
  override val supportedRuntime = HetznerProviderConfigurationRuntime::class

  override fun createConfigurationManager() = HetznerProviderConfigurationManager()

  override fun createConfigurationFactory() = HetznerProviderConfigurationFactory()

  override val type = HETZNER_PROVIDER_TYPE
}
