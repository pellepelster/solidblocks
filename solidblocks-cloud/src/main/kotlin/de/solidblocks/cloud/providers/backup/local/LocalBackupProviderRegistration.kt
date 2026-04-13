package de.solidblocks.cloud.providers.backup.local

import de.solidblocks.cloud.providers.types.backup.BackupProviderRegistration

val BACKUP_LOCAL_TYPE = "backup_local"

class LocalBackupProviderRegistration :
    BackupProviderRegistration<
        LocalBackupProviderConfiguration,
        LocalBackupProviderConfigurationRuntime,
        LocalBackupProviderManager,
        > {
    override val supportedConfiguration = LocalBackupProviderConfiguration::class
    override val supportedRuntime = LocalBackupProviderConfigurationRuntime::class

    override fun createManager() = LocalBackupProviderManager()

    override fun createFactory() = LocalBackupProviderConfigurationFactory()

    override val type = BACKUP_LOCAL_TYPE
}
