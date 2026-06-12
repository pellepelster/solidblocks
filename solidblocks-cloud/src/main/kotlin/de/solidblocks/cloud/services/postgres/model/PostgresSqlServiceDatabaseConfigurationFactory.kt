package de.solidblocks.cloud.services.postgres.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.ConfigurationFactory
import de.solidblocks.cloud.configuration.ListKeyword
import de.solidblocks.cloud.configuration.StringConstraints.Companion.DOMAIN_NAME
import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.configuration.constraints
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.result
import io.github.oshai.kotlinlogging.KotlinLogging

class PostgresSqlServiceDatabaseConfigurationFactory : ConfigurationFactory<PostgresSqlServiceDatabaseConfiguration> {

    private val logger = KotlinLogging.logger {}

    val name =
        StringKeyword(
            "name",
            KeywordHelp(
                "Unique name for the database",
            ),
        ).constraints(DOMAIN_NAME)

    val users =
        ListKeyword(
            "users",
            PostgresSqlServiceDatabaseUserConfigurationFactory(),
            KeywordHelp("Users to create for database access. Users that are removed from this list will not be deleted automatically and their privileges will not be revoked."),
        )

    override val help: ConfigurationHelp
        get() = TODO("Not yet implemented")

    override val keywords = listOf(name, users)

    override fun parse(yaml: YamlNode): Result<PostgresSqlServiceDatabaseConfiguration> = result {
        PostgresSqlServiceDatabaseConfiguration(
            name.parse(yaml).bind(),
            users.parse(yaml).bind(),
        )
    }
}
