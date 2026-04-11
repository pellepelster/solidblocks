package de.solidblocks.cloud.providers.backup.local

import de.solidblocks.cloud.providers.types.backup.BackupProviderConfiguration

data class LocalBackupProviderConfiguration(override val name: String) : BackupProviderConfiguration {
    override val type = BACKUP_LOCAL_TYPE
}
