package de.solidblocks.cloud.providers.backup.local

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.providers.CloudConfigurationContext
import de.solidblocks.cloud.providers.types.backup.BackupProviderConfigurationManager
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging

class LocalBackupProviderManager :
    BackupProviderConfigurationManager<LocalBackupProviderConfiguration, LocalBackupProviderConfigurationRuntime> {

    private val logger = KotlinLogging.logger {}

    override fun validate(configuration: LocalBackupProviderConfiguration, context: CloudConfigurationContext, log: LogContext) = Success(LocalBackupProviderConfigurationRuntime())

    override fun createProvisioners(runtime: LocalBackupProviderConfigurationRuntime) = emptyList<InfrastructureResourceProvisioner<*, *>>()

    override val supportedConfiguration = LocalBackupProviderConfiguration::class
}
