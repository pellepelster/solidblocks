package de.solidblocks.cloud.services

import de.solidblocks.cloud.configuration.NumberConstraints.Companion.NONE
import de.solidblocks.cloud.configuration.NumberConstraints.Companion.VOLUME_SIZE
import de.solidblocks.cloud.configuration.NumberKeywordOptional
import de.solidblocks.cloud.configuration.NumberKeywordOptionalWithDefault
import de.solidblocks.cloud.configuration.StringConstraints.Companion.RFC_1123_NAME
import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.utils.KeywordHelp

val SERVICE_NAME_KEYWORD =
    StringKeyword(
        "name",
        RFC_1123_NAME,
        KeywordHelp(
            "Unique name for the service. Must conform with [RFC 1123](https://datatracker.ietf.org/doc/html/rfc1123) to ensure it can be used as part of a domain name.",
        ),
    )

val SERVICE_DATA_VOLUME_SIZE_KEYWORD =
    NumberKeywordOptionalWithDefault(
        "data_size",
        VOLUME_SIZE,
        KeywordHelp(
            "Size in GB for the data volume keeping all data needed for this service.",
        ),
        16,
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

val BACKUP_DATA_VOLUME_SIZE_KEYWORD =
    NumberKeywordOptional(
        "backup_size",
        VOLUME_SIZE,
        KeywordHelp(
            "Size in GB for the local backup volume. If not set the size will be derived from the data volume size and the amount of full backup retention days.",
        ),
    )
