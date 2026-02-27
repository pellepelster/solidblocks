package de.solidblocks.cloud.services.s3.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.*
import de.solidblocks.cloud.configuration.StringConstraints.Companion.DOMAIN_NAME
import de.solidblocks.cloud.configuration.StringConstraints.Companion.NONE
import de.solidblocks.cloud.configuration.StringConstraints.Companion.RFC_1123_NAME
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
            DOMAIN_NAME,
            KeywordHelp(
                "Unique name for the bucket. Must conform with [RFC 1123](https://datatracker.ietf.org/doc/html/rfc1123) to ensure it can be used as part of a domain name.",
            ),
        )

    val publicAccess =
        OptionalBooleanKeyword(
            "public_access",
            KeywordHelp(
                "If enabled the bucket content will be publicly available via 'https' without any authentication",
            ),
            false,
        )

    val accessKeys =
        ListKeyword("access_keys", S3ServiceBucketAccessKeyConfigurationFactory(), KeywordHelp("Access keys to generate for bucket access"))

    override val help: ConfigurationHelp
        get() = TODO("Not yet implemented")

    override val keywords = listOf(name, publicAccess, accessKeys)

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

        val accessKeys =
            when (val result = accessKeys.parse(yaml)) {
                is Error<List<S3ServiceBucketAccessKeyConfiguration>> -> return Error(result.error)
                is Success<List<S3ServiceBucketAccessKeyConfiguration>> -> result.data
            }


        logger.debug { "parsed bucket '$name', publicAccess: $publicAccess" }
        return Success(S3ServiceBucketConfiguration(name, publicAccess, accessKeys))
    }
}
