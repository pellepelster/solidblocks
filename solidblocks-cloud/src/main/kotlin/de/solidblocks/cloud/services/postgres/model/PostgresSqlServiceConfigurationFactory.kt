package de.solidblocks.cloud.services.postgres.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.KeywordHelp
import de.solidblocks.cloud.configuration.ListKeyword
import de.solidblocks.cloud.configuration.NumberConstraints.Companion.NONE
import de.solidblocks.cloud.configuration.NumberKeywordOptionalWithDefault
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.services.SERVICE_DATA_VOLUME_SIZE_KEYWORD
import de.solidblocks.cloud.services.SERVICE_NAME_KEYWORD
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success

class PostgresSqlServiceConfigurationFactory : PolymorphicConfigurationFactory<PostgresSqlServiceConfiguration>() {

    val databases =
        ListKeyword("databases", PostgresSqlServiceDatabaseConfigurationFactory(), KeywordHelp("List of databases to create. Databases that are removed from this list will not be deleted automatically."))

    override val help = ConfigurationHelp("PostgreSQL", "Single node PostgreSQL database instance with pgBackRest powered backup.")

    val instanceSize =
        NumberKeywordOptionalWithDefault(
            "instanceSize",
            NONE,
            KeywordHelp(
                "Size in GB for the database instance",
            ),
            16,
        )

    val backupFullRetentionDays =
        NumberKeywordOptionalWithDefault(
            "backupFullRetentionDays",
            NONE,
            KeywordHelp(
                "amount of days to keep full backups",
            ),
            16,
        )

    override val keywords = listOf(SERVICE_NAME_KEYWORD, databases, instanceSize, backupFullRetentionDays)

    override fun parse(yaml: YamlNode): Result<PostgresSqlServiceConfiguration> {
        val name =
            when (val name = SERVICE_NAME_KEYWORD.parse(yaml)) {
                is Error<*> -> return Error(name.error)
                is Success<String> -> name.data
            }

        val backupFullRetentionDays =
            when (val name = backupFullRetentionDays.parse(yaml)) {
                is Error<*> -> return Error(name.error)
                is Success<Int> -> name.data
            }

        val instanceSize =
            when (val name = instanceSize.parse(yaml)) {
                is Error<*> -> return Error(name.error)
                is Success<Int> -> name.data
            }

        val databases =
            when (val result = databases.parse(yaml)) {
                is Error<List<PostgresSqlServiceDatabaseConfiguration>> -> return Error(result.error)
                is Success<List<PostgresSqlServiceDatabaseConfiguration>> -> result.data
            }

        return Success(PostgresSqlServiceConfiguration(name, instanceSize, backupFullRetentionDays, databases))
    }
}
