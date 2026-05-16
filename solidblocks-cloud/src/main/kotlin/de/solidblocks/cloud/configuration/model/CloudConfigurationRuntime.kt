package de.solidblocks.cloud.configuration.model

import de.solidblocks.cloud.providers.CloudConfigurationContext
import de.solidblocks.cloud.providers.ProviderConfigurationRuntime
import de.solidblocks.cloud.providers.github.GithubProviderRuntime
import de.solidblocks.cloud.providers.hetzner.HetznerProviderRuntime
import de.solidblocks.cloud.providers.types.backup.BackupProviderConfigurationRuntime
import de.solidblocks.cloud.services.ServiceConfigurationRuntime

data class CloudConfigurationRuntime(
    val context: CloudConfigurationContext,
    val environmentContext: EnvironmentContext,
    val environmentVars: Map<String, String>,
    val rootDomain: String?,
    val providers: List<ProviderConfigurationRuntime>,
    val services: List<ServiceConfigurationRuntime>,
) {
    // TODO clean up when multi provider support is needed
    fun hetznerProviderRuntime() = providers.filterIsInstance<HetznerProviderRuntime>().single()

    fun backupProviderRuntime() = providers.filterIsInstance<BackupProviderConfigurationRuntime>().single()

    fun githubProviderRuntime() = providers.filterIsInstance<GithubProviderRuntime>().single()

    val dnsEnabled: Boolean
        get() = rootDomain != null
}
