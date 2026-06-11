package de.solidblocks.cloud.services

import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.configuration.default
import de.solidblocks.cloud.providers.DEFAULT_NAME
import de.solidblocks.cloud.utils.KeywordHelp

val PROVIDER_NAME_KEYWORD =
    StringKeyword(
        "name",
        KeywordHelp(
            "Name for the provider, can be omitted if only one provider of this specific type is configured",
        ),
    ).default(DEFAULT_NAME)
