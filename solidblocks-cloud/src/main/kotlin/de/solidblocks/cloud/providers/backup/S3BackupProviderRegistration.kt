package de.solidblocks.cloud.providers.backup

import de.solidblocks.cloud.providers.ProviderRegistration

val BACKUP_S3_TYPE = "backup_s3"

class S3BackupProviderRegistration :
    ProviderRegistration<
        S3BackupProviderConfiguration,
        S3BackupProviderRuntime,
        S3BackupProviderManager,
        > {
    override val supportedConfiguration = S3BackupProviderConfiguration::class
    override val supportedRuntime = S3BackupProviderRuntime::class

    override fun createConfigurationManager() = S3BackupProviderManager()

    override fun createConfigurationFactory() = S3BackupProviderConfigurationFactory()

    override val type = BACKUP_S3_TYPE
}
