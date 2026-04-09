package de.solidblocks.cloud.services.postgres.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.ListKeyword
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.services.BackupConfig
import de.solidblocks.cloud.services.BackupConfigurationFactory
import de.solidblocks.cloud.services.InstanceConfig
import de.solidblocks.cloud.services.InstanceConfigurationFactory
import de.solidblocks.cloud.services.SERVICE_NAME_KEYWORD
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

    override val help =
        ConfigurationHelp(
            "PostgreSQL",
            "Single node PostgreSQL database instance with pgBackRest powered backup.",
        )

    override val keywords =
        listOf(
            SERVICE_NAME_KEYWORD,
            databases,
        ) + BackupConfigurationFactory.keywords + InstanceConfigurationFactory.keywords

    override fun parse(yaml: YamlNode): Result<PostgresSqlServiceConfiguration> {
        val name =
            when (val result = SERVICE_NAME_KEYWORD.parse(yaml)) {
                is Error<*> -> return Error(result.error)
                is Success<String> -> result.data
            }

        val instance =
            when (val result = InstanceConfigurationFactory.parse(yaml)) {
                is Error<InstanceConfig> -> return Error(result.error)
                is Success<InstanceConfig> -> result.data
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

        return Success(
            PostgresSqlServiceConfiguration(
                name,
                instance,
                backupConfig,
                databases,
            ),
        )
    }
}
