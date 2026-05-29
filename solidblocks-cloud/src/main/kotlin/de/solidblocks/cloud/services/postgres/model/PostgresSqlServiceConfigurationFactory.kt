package de.solidblocks.cloud.services.postgres.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.ListKeyword
import de.solidblocks.cloud.configuration.NumberConstraints
import de.solidblocks.cloud.configuration.NumberKeywordOptionalWithDefault
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.services.*
import de.solidblocks.cloud.services.ServiceConfigurationFactory.parseServiceCommonConfig
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.result

class PostgresSqlServiceConfigurationFactory : PolymorphicConfigurationFactory<PostgresSqlServiceConfiguration>() {

    val databases =
        ListKeyword(
            "databases",
            PostgresSqlServiceDatabaseConfigurationFactory(),
            KeywordHelp(
                "List of databases to create. Databases that are removed from this list will not be deleted automatically.",
            ),
        )

    val majorVersion =
        NumberKeywordOptionalWithDefault(
            "majorVersion",
            NumberConstraints(18, 14),
            KeywordHelp(
                "Postgres major version",
            ),
            17,
        )

    override val help =
        ConfigurationHelp(
            "PostgreSQL",
            "Single node PostgreSQL database instance with pgBackRest powered backup.",
        )

    override val keywords =
        listOf(
            majorVersion,
            databases,
        ) + BackupConfigurationFactory.keywords + ServiceConfigurationFactory.keywords

    override fun parse(yaml: YamlNode): Result<PostgresSqlServiceConfiguration> = result {
        PostgresSqlServiceConfiguration(
            yaml.parseServiceCommonConfig().bind(),
            BackupConfigurationFactory.parse(yaml).bind(),
            databases.parse(yaml).bind(),
            majorVersion.parse(yaml).bind(),
        )
    }
}
