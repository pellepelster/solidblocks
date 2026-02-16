package de.solidblocks.cloud.configuration.model

import de.solidblocks.cloud.providers.DEFAULT_NAME
import de.solidblocks.cloud.providers.ProviderConfiguration
import de.solidblocks.cloud.services.ServiceConfiguration

data class CloudConfiguration(
    val name: String,
    val rootDomain: String,
    val providers: List<ProviderConfiguration>,
    val services: List<ServiceConfiguration>,
) {
  fun getDefaultEnvironment() = DEFAULT_NAME
}
