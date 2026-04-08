package de.solidblocks.cloud.services

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.Keyword
import de.solidblocks.cloud.configuration.NumberConstraints.Companion.NONE
import de.solidblocks.cloud.configuration.NumberConstraints.Companion.VOLUME_SIZE
import de.solidblocks.cloud.configuration.NumberKeywordOptional
import de.solidblocks.cloud.configuration.NumberKeywordOptionalWithDefault
import de.solidblocks.cloud.utils.ByteSize
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success

object BackupConfigurationFactory {

  val BACKUP_DATA_VOLUME_SIZE_KEYWORD =
      NumberKeywordOptional(
          "backup_size",
          VOLUME_SIZE,
          KeywordHelp(
              "Size in GB for the local backup volume. If not set the size will be derived from the data volume size and the amount of full backup retention days.",
          ),
      )

  val BACKUP_FULL_RETENTION_DAYS_KEYWORD =
      NumberKeywordOptionalWithDefault(
          "backup_full_retention_days",
          NONE,
          KeywordHelp(
              "amount of days to keep full backups",
          ),
          7,
      )

  val keywords =
      listOf<Keyword<*>>(BACKUP_DATA_VOLUME_SIZE_KEYWORD, BACKUP_FULL_RETENTION_DAYS_KEYWORD)

  fun parse(yaml: YamlNode): Result<BackupConfig> {
    val backupVolumeSize =
        when (val result = BACKUP_DATA_VOLUME_SIZE_KEYWORD.parse(yaml)) {
          is Error<Int?> -> return Error(result.error)
          is Success<Int?> -> result.data
        }

    val backupFullRetentionDays =
        when (val result = BACKUP_FULL_RETENTION_DAYS_KEYWORD.parse(yaml)) {
          is Error<Int> -> return Error(result.error)
          is Success<Int> -> result.data
        }

    return Success(BackupConfig(backupVolumeSize, backupFullRetentionDays))
  }
}

data class BackupConfig(val volumeSize: Int?, val fullRetentionDays: Int)

data class BackupRuntime(val volumeSize: Int?, val fullRetentionDays: Int) {
  companion object {
    fun fromConfig(config: BackupConfig) =
        BackupRuntime(config.volumeSize, config.fullRetentionDays)
  }

  fun backupVolumeSizeWithDefault(dataVolumeSize: Int) =
      ByteSize.fromGigabytes(
          volumeSize ?: (dataVolumeSize * fullRetentionDays),
      )
}
