package de.solidblocks.cloud.providers.backup.aws

import de.solidblocks.cloud.providers.types.backup.BackupProviderConfiguration

data class S3BackupProviderConfiguration(override val name: String, val region: String) : BackupProviderConfiguration {
    override val type = BACKUP_S3_TYPE
}
