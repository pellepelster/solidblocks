package de.solidblocks.cloud.configuration.model

import de.solidblocks.cloud.providers.DEFAULT_NAME
import de.solidblocks.cloud.providers.hetzner.HetznerProviderConfiguration

data class CloudConfigurationRuntime(
    val name: String,
    val rootDomain: String?,
) {
    fun getDefaultEnvironment() = DEFAULT_NAME
}
