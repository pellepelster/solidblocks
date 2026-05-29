package de.solidblocks.cloud.services.s3.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.ConfigurationFactory
import de.solidblocks.cloud.configuration.ListKeyword
import de.solidblocks.cloud.configuration.OptionalBooleanKeyword
import de.solidblocks.cloud.configuration.StringConstraints.Companion.DOMAIN_NAME
import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.configuration.StringListKeyword
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.result
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

    val publicAccessDomains =
        StringListKeyword(
            "public_access_domains",
            KeywordHelp(
                "If '${publicAccess.name}' is enabled the bucket will also listen on these Domains. Requires A/AAAA entries to point to the server hosting the buckets. If any provider supports those domains the entries will automatically be created.",
            ),
        )
    val accessKeys =
        ListKeyword(
            "access_keys",
            S3ServiceBucketAccessKeyConfigurationFactory(),
            KeywordHelp("Access keys to generate for bucket access"),
        )

    override val help: ConfigurationHelp
        get() = TODO("Not yet implemented")

    override val keywords = listOf(name, publicAccess, accessKeys, publicAccessDomains)

    override fun parse(yaml: YamlNode): Result<S3ServiceBucketConfiguration> = result {
        val name = name.parse(yaml).bind()
        val publicAccess = publicAccess.parse(yaml).bind()
        val publicAccessDomains = publicAccessDomains.parse(yaml).bind()
        val accessKeys = accessKeys.parse(yaml).bind()

        logger.debug {
            "parsed bucket '$name', publicAccess: $publicAccess, publicAccessDomains: ${publicAccessDomains.joinToString(", ")}"
        }

        S3ServiceBucketConfiguration(name, publicAccess, accessKeys, publicAccessDomains)
    }
}
