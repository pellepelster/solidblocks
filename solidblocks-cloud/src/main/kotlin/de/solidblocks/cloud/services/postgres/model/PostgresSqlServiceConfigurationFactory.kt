package de.solidblocks.cloud.services.postgres.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.ListKeyword
import de.solidblocks.cloud.configuration.NumberConstraints
import de.solidblocks.cloud.configuration.NumberKeywordOptionalWithDefault
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.services.*
import de.solidblocks.cloud.services.ServiceConfigurationFactory.parseServiceCommonConfig
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success

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

    override fun parse(yaml: YamlNode): Result<PostgresSqlServiceConfiguration> {
        val common = when (val result = yaml.parseServiceCommonConfig()) {
            is Error<ServiceCommonConfig> -> return Error(result.error)
            is Success<ServiceCommonConfig> -> result.data
        }

        val backupConfig =
            when (val result = BackupConfigurationFactory.parse(yaml)) {
                is Error<BackupConfig> -> return Error(result.error)
                is Success<BackupConfig> -> result.data
            }

        val databases =
            when (val result = databases.parse(yaml)) {
                is Error<List<PostgresSqlServiceDatabaseConfiguration>> -> return Error(result.error)
                is Success<List<PostgresSqlServiceDatabaseConfiguration>> -> result.data
            }

        val majorVersion =
            when (val result = majorVersion.parse(yaml)) {
                is Error<Int> -> return Error(result.error)
                is Success<Int> -> result.data
            }

        return Success(
            PostgresSqlServiceConfiguration(
                common.name,
                common.environmentVars,
                common.instance,
                backupConfig,
                databases,
                majorVersion,
            ),
        )
    }
}
