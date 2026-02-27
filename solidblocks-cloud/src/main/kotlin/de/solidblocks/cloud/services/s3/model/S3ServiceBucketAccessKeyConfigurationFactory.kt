package de.solidblocks.cloud.services.s3.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.ConfigurationFactory
import de.solidblocks.cloud.configuration.KeywordHelp
import de.solidblocks.cloud.configuration.OptionalBooleanKeyword
import de.solidblocks.cloud.configuration.SimpleKeyword
import de.solidblocks.cloud.configuration.StringConstraints.Companion.NONE
import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import io.github.oshai.kotlinlogging.KotlinLogging

class S3ServiceBucketAccessKeyConfigurationFactory : ConfigurationFactory<S3ServiceBucketAccessKeyConfiguration> {

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

    val owner =
        OptionalBooleanKeyword(
            "owner",
            KeywordHelp(
                "Grant owner permission to the access key",
            ),
            false
        )

    val read =
        OptionalBooleanKeyword(
            "read",
            KeywordHelp(
                "Grant read permission to the access key",
            ),
            false
        )
    val write =
        OptionalBooleanKeyword(
            "read",
            KeywordHelp(
                "Grant write permission to the access key",
            ),
            false
        )

    override val help: ConfigurationHelp
        get() = TODO("Not yet implemented")

    override val keywords = listOf<SimpleKeyword<*>>(name, owner, read, write)

    override fun parse(yaml: YamlNode): Result<S3ServiceBucketAccessKeyConfiguration> {
        val name =
            when (val result = name.parse(yaml)) {
                is Error<*> -> return Error(result.error)
                is Success<String> -> result.data
            }

        return Success(S3ServiceBucketAccessKeyConfiguration(name))
    }
}
