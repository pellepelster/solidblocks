package de.solidblocks.cloud.providers.backup.local

import de.solidblocks.cloud.api.ResourceProvisioner
import de.solidblocks.cloud.api.Success
import de.solidblocks.cloud.api.log.LogContext
import de.solidblocks.cloud.providers.CloudConfigurationContext
import de.solidblocks.cloud.providers.types.backup.BackupProviderManager
import io.github.oshai.kotlinlogging.KotlinLogging

class LocalBackupProviderManager :
    BackupProviderManager<LocalBackupProviderConfiguration, LocalBackupProviderConfigurationRuntime> {

    private val logger = KotlinLogging.logger {}

    override fun validateConfiguration(configuration: LocalBackupProviderConfiguration, context: CloudConfigurationContext, log: LogContext) = Success(LocalBackupProviderConfigurationRuntime())

    override fun createProvisioners(runtime: LocalBackupProviderConfigurationRuntime) = emptyList<ResourceProvisioner<*, *, *>>()

    override val supportedConfiguration = LocalBackupProviderConfiguration::class
}
