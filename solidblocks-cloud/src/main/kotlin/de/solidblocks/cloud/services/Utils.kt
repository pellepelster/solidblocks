package de.solidblocks.cloud.services

import de.solidblocks.cloud.configuration.StringConstraints.Companion.NONE
import de.solidblocks.cloud.configuration.StringConstraints.Companion.RFC_1123_NAME
import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.configuration.StringKeywordOptionalWithDefault
import de.solidblocks.cloud.providers.DEFAULT_NAME
import de.solidblocks.cloud.utils.KeywordHelp

val SERVICE_NAME_KEYWORD =
    StringKeyword(
        "name",
        RFC_1123_NAME,
        KeywordHelp(
            "Unique name for the service. Must conform with [RFC 1123](https://datatracker.ietf.org/doc/html/rfc1123) to ensure it can be used as part of a domain name.",
        ),
    )

val PROVIDER_NAME_KEYWORD =
    StringKeywordOptionalWithDefault(
        "name",
        NONE,
        DEFAULT_NAME,
        KeywordHelp(
            "Name for the provider, can be omitted if only one provider of this specific type is configured",
        ),
    )

