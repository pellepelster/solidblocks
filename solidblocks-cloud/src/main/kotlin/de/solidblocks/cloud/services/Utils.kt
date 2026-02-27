package de.solidblocks.cloud.services

import de.solidblocks.cloud.configuration.KeywordHelp
import de.solidblocks.cloud.configuration.NumberConstraints.Companion.NONE
import de.solidblocks.cloud.configuration.NumberKeywordOptionalWithDefault
import de.solidblocks.cloud.configuration.StringConstraints.Companion.RFC_1123_NAME
import de.solidblocks.cloud.configuration.StringKeyword

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
        "size",
        NONE,
        KeywordHelp(
            "Size in GB for the data volume",
        ),
        16,
    )

