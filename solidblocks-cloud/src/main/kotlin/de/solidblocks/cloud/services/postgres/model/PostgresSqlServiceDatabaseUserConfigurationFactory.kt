package de.solidblocks.cloud.services.postgres.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.*
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.result
import io.github.oshai.kotlinlogging.KotlinLogging

class PostgresSqlServiceDatabaseUserConfigurationFactory : ConfigurationFactory<PostgresSqlServiceDatabaseUserConfiguration> {

    private val logger = KotlinLogging.logger {}

    val name =
        StringKeyword(
            "name",
            KeywordHelp(
                "Unique name for the database user",
            ),
        )

    val admin =
        BooleanKeyword(
            "admin",
            KeywordHelp(
                "Grant full DDL privileges to the user",
            ),
        ).default(false)

    val read =
        BooleanKeyword(
            "read",
            KeywordHelp(
                "Grant read permissions to the user",
            ),
        ).default(false)
    val write =
        BooleanKeyword(
            "write",
            KeywordHelp(
                "Grant update/insert and delete permissions to the user",
            ),
        ).default(false)

    override val help: ConfigurationHelp
        get() = TODO("Not yet implemented")

    override val keywords = listOf<SimpleKeyword<*>>(name, admin, read, write)

    override fun parse(yaml: YamlNode): Result<PostgresSqlServiceDatabaseUserConfiguration> = result {
        PostgresSqlServiceDatabaseUserConfiguration(
            name.parse(yaml).bind(),
            admin.parse(yaml).bind(),
            read.parse(yaml).bind(),
            write.parse(yaml).bind(),
        )
    }
}
