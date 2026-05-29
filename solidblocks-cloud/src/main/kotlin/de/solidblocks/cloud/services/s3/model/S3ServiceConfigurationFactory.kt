package de.solidblocks.cloud.services.s3.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.ListKeyword
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.services.*
import de.solidblocks.cloud.services.ServiceConfigurationFactory.parseServiceCommonConfig
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.result

class S3ServiceConfigurationFactory : PolymorphicConfigurationFactory<S3ServiceConfiguration>() {

    val buckets =
        ListKeyword(
            "buckets",
            S3ServiceBucketConfigurationFactory(),
            KeywordHelp(
                "List of S3 buckets to create. Buckets that are removed from this list will not be deleted automatically.",
            ),
        )

    override val help =
        ConfigurationHelp(
            "S3",
            "S3 compatible object storage service based on [GarageFS](https://garagehq.deuxfleurs.fr/). Currently only single region deployment are supported.",
        )

    override val keywords =
        listOf(
            buckets,
        ) + BackupConfigurationFactory.keywords + ServiceConfigurationFactory.keywords

    override fun parse(yaml: YamlNode): Result<S3ServiceConfiguration> = result {
        S3ServiceConfiguration(
            yaml.parseServiceCommonConfig().bind(),
            BackupConfigurationFactory.parse(yaml).bind(),
            buckets.parse(yaml).bind(),
        )
    }
}
