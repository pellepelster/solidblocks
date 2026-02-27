package de.solidblocks.cloud.services.s3.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.KeywordHelp
import de.solidblocks.cloud.configuration.ListKeyword
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.services.SERVICE_DATA_VOLUME_SIZE_KEYWORD
import de.solidblocks.cloud.services.SERVICE_NAME_KEYWORD
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success

class S3ServiceConfigurationFactory : PolymorphicConfigurationFactory<S3ServiceConfiguration>() {

    val buckets =
        ListKeyword("buckets", S3ServiceBucketConfigurationFactory(), KeywordHelp("List of S3 buckets to create. Buckets that are removed from this list will not be deleted automatically."))

    override val help = ConfigurationHelp("S3", "S3 compatible object storage service based on [GarageFS](https://garagehq.deuxfleurs.fr/). Currently only single region deployment are supported.")

    override val keywords = listOf(SERVICE_NAME_KEYWORD, buckets, SERVICE_DATA_VOLUME_SIZE_KEYWORD)

    override fun parse(yaml: YamlNode): Result<S3ServiceConfiguration> {
        val name =
            when (val name = SERVICE_NAME_KEYWORD.parse(yaml)) {
                is Error<*> -> return Error(name.error)
                is Success<String> -> name.data
            }

        val buckets =
            when (val result = this@S3ServiceConfigurationFactory.buckets.parse(yaml)) {
                is Error<List<S3ServiceBucketConfiguration>> -> return Error(result.error)
                is Success<List<S3ServiceBucketConfiguration>> -> result.data
            }

        return Success(S3ServiceConfiguration(name, buckets))
    }
}
