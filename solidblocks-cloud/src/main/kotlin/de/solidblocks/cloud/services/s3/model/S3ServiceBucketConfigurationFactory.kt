package de.solidblocks.cloud.services.s3.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.*
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import io.github.oshai.kotlinlogging.KotlinLogging

class S3ServiceBucketConfigurationFactory : ConfigurationFactory<S3ServiceBucketConfiguration> {

    private val logger = KotlinLogging.logger {}

    val name =
        StringKeyword(
            "name",
            KeywordHelp(
                "TODO",
                "TODO",
            ),
        )

    val publicAccess =
        OptionalBooleanKeyword(
            "public_access",
            KeywordHelp(
                "TODO",
                "TODO",
            ),
            false,
        )

    override val help: ConfigurationHelp
        get() = TODO("Not yet implemented")

    override val keywords = listOf<SimpleKeyword<*>>(name, publicAccess)

    override fun parse(yaml: YamlNode): Result<S3ServiceBucketConfiguration> {
        val name =
            when (val result = name.parse(yaml)) {
                is Error<*> -> return Error(result.error)
                is Success<String> -> result.data
            }

        val publicAccess =
            when (val result = publicAccess.parse(yaml)) {
                is Error<*> -> return Error(result.error)
                is Success<Boolean> -> result.data
            }

        logger.debug { "parsed bucket '$name', publicAccess: $publicAccess" }
        return Success(S3ServiceBucketConfiguration(name, publicAccess))
    }
}
