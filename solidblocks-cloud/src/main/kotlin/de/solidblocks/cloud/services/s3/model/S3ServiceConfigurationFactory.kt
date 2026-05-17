package de.solidblocks.cloud.services.s3.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.ListKeyword
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.services.*
import de.solidblocks.cloud.services.ServiceConfigurationFactory.parseServiceCommonConfig
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success

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

    override fun parse(yaml: YamlNode): Result<S3ServiceConfiguration> {
        val common = when (val result = yaml.parseServiceCommonConfig()) {
            is Error<ServiceCommonConfig> -> return Error(result.error)
            is Success<ServiceCommonConfig> -> result.data
        }

        val buckets =
            when (val result = buckets.parse(yaml)) {
                is Error<List<S3ServiceBucketConfiguration>> -> return Error(result.error)
                is Success<List<S3ServiceBucketConfiguration>> -> result.data
            }

        val backup =
            when (val result = BackupConfigurationFactory.parse(yaml)) {
                is Error<BackupConfig> -> return Error(result.error)
                is Success<BackupConfig> -> result.data
            }

        return Success(S3ServiceConfiguration(common.name, common.environmentVars, common.instance, backup, buckets))
    }
}
