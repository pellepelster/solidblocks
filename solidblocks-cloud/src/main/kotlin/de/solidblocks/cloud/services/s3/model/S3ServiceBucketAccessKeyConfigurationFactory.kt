package de.solidblocks.cloud.services.s3.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.BooleanKeyword
import de.solidblocks.cloud.configuration.ConfigurationFactory
import de.solidblocks.cloud.configuration.SimpleKeyword
import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.configuration.default
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.result
import io.github.oshai.kotlinlogging.KotlinLogging

class S3ServiceBucketAccessKeyConfigurationFactory : ConfigurationFactory<S3ServiceBucketAccessKeyConfiguration> {

    private val logger = KotlinLogging.logger {}

    // TODO verify size and content
    val name =
        StringKeyword(
            "name",
            KeywordHelp(
                "Unique name for the access key",
            ),
        )

    val owner =
        BooleanKeyword(
            "owner",
            KeywordHelp(
                "Grant owner permission to the access key",
            ),
        ).default(false)

    val read =
        BooleanKeyword(
            "read",
            KeywordHelp(
                "Grant read permission to the access key",
            ),
        ).default(false)

    val write =
        BooleanKeyword(
            "write",
            KeywordHelp(
                "Grant write permission to the access key",
            ),
        ).default(false)

    override val help: ConfigurationHelp
        get() = TODO("Not yet implemented")

    override val keywords = listOf<SimpleKeyword<*>>(name, owner, read, write)

    override fun parse(yaml: YamlNode): Result<S3ServiceBucketAccessKeyConfiguration> = result {
        S3ServiceBucketAccessKeyConfiguration(
            name.parse(yaml).bind(),
            owner.parse(yaml).bind(),
            read.parse(yaml).bind(),
            write.parse(yaml).bind(),
        )
    }
}
