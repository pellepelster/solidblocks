package de.solidblocks.cloud.services.postgres.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.ConfigurationFactory
import de.solidblocks.cloud.configuration.ListKeyword
import de.solidblocks.cloud.configuration.StringConstraints.Companion.DOMAIN_NAME
import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import io.github.oshai.kotlinlogging.KotlinLogging

class PostgresSqlServiceDatabaseConfigurationFactory : ConfigurationFactory<PostgresSqlServiceDatabaseConfiguration> {

    private val logger = KotlinLogging.logger {}

    val name =
        StringKeyword(
            "name",
            DOMAIN_NAME,
            KeywordHelp(
                "Unique name for the database",
            ),
        )

    val users =
        ListKeyword(
            "users",
            PostgresSqlServiceDatabaseUserConfigurationFactory(),
            KeywordHelp("Users to create for database access"),
        )

    override val help: ConfigurationHelp
        get() = TODO("Not yet implemented")

    override val keywords = listOf(name, users)

    override fun parse(yaml: YamlNode): Result<PostgresSqlServiceDatabaseConfiguration> {
        val name =
            when (val result = name.parse(yaml)) {
                is Error<*> -> return Error(result.error)
                is Success<String> -> result.data
            }

        val users =
            when (val result = users.parse(yaml)) {
                is Error<List<PostgresSqlServiceDatabaseUserConfiguration>> -> return Error(result.error)
                is Success<List<PostgresSqlServiceDatabaseUserConfiguration>> -> result.data
            }

        return Success(PostgresSqlServiceDatabaseConfiguration(name, users))
    }
}
