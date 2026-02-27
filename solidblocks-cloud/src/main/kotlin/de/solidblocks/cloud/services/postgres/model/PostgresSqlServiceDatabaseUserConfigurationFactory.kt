package de.solidblocks.cloud.services.postgres.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.*
import de.solidblocks.cloud.configuration.StringConstraints.Companion.NONE
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import io.github.oshai.kotlinlogging.KotlinLogging

class PostgresSqlServiceDatabaseUserConfigurationFactory : ConfigurationFactory<PostgresSqlServiceDatabaseUserConfiguration> {

    private val logger = KotlinLogging.logger {}

    // TODO verify size and content
    val name =
        StringKeyword(
            "name",
            NONE,
            KeywordHelp(
                "Unique name for the access key",
            ),
        )


    val admin =
        OptionalBooleanKeyword(
            "admin",
            KeywordHelp(
                "Grant full DDL privileges to the user",
            ),
            false
        )

    val read =
        OptionalBooleanKeyword(
            "read",
            KeywordHelp(
                "Grant read permissions to the user",
            ),
            false
        )
    val write =
        OptionalBooleanKeyword(
            "write",
            KeywordHelp(
                "Grant update/insert and delete permissions to the user",
            ),
            false
        )


    override val help: ConfigurationHelp
        get() = TODO("Not yet implemented")

    override val keywords = listOf<SimpleKeyword<*>>(name, admin, read, write)

    override fun parse(yaml: YamlNode): Result<PostgresSqlServiceDatabaseUserConfiguration> {
        val name =
            when (val result = name.parse(yaml)) {
                is Error<*> -> return Error(result.error)
                is Success<String> -> result.data
            }

        return Success(PostgresSqlServiceDatabaseUserConfiguration(name))
    }
}
