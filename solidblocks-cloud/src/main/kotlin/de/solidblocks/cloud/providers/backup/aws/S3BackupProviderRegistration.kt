package de.solidblocks.cloud.providers.backup.aws

import de.solidblocks.cloud.providers.types.backup.BackupProviderRegistration

val BACKUP_S3_TYPE = "backup_aws_s3"

class S3BackupProviderRegistration :
    BackupProviderRegistration<
        S3BackupProviderConfiguration,
        S3BackupProviderConfigurationRuntime,
        S3BackupProviderManager,
        > {
    override val supportedConfiguration = S3BackupProviderConfiguration::class
    override val supportedRuntime = S3BackupProviderConfigurationRuntime::class

    override fun createManager() = S3BackupProviderManager()

    override fun createFactory() = S3BackupProviderConfigurationFactory()

    override val type = BACKUP_S3_TYPE
}
