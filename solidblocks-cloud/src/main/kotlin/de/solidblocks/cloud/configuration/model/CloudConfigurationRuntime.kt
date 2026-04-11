package de.solidblocks.cloud.configuration.model

import de.solidblocks.cloud.providers.CloudConfigurationContext
import de.solidblocks.cloud.providers.DEFAULT_NAME
import de.solidblocks.cloud.providers.ProviderConfigurationRuntime
import de.solidblocks.cloud.providers.hetzner.HetznerProviderRuntime
import de.solidblocks.cloud.providers.types.backup.BackupProviderConfigurationRuntime
import de.solidblocks.cloud.services.ServiceConfigurationRuntime

data class CloudConfigurationRuntime(
    val context: CloudConfigurationContext,
    val name: String,
    val rootDomain: String?,
    val providers: List<ProviderConfigurationRuntime>,
    val services: List<ServiceConfigurationRuntime>,
) {
    fun getDefaultEnvironment() = DEFAULT_NAME

    // TODO clean up when multi provider support is needed
    fun hetznerProviderRuntime() = providers.filterIsInstance<HetznerProviderRuntime>().single()

    fun backupProviderRuntime() = providers.filterIsInstance<BackupProviderConfigurationRuntime>().single()

    val dnsEnabled: Boolean
        get() = rootDomain != null
}
