package de.solidblocks.cloud.providers.backup

import de.solidblocks.cloud.providers.ProviderConfiguration


data class S3BackupProviderConfiguration(override val name: String, val region: String) : ProviderConfiguration {
    override val type = BACKUP_S3_TYPE
}
