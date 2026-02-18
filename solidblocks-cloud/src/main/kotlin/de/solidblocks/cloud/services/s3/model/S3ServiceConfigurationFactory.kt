package de.solidblocks.cloud.services.s3.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.KeywordHelp
import de.solidblocks.cloud.configuration.ListKeyword
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success

class S3ServiceConfigurationFactory : PolymorphicConfigurationFactory<S3ServiceConfiguration>() {

  val name =
      StringKeyword(
          "name",
          KeywordHelp(
              "TODO",
              "TODO",
          ),
      )

  val buckets =
      ListKeyword("buckets", S3ServiceBucketConfigurationFactory(), KeywordHelp("TODO", "TODO"))

  override val help: ConfigurationHelp
    get() = TODO("Not yet implemented")

  override val keywords = listOf(name, buckets)

  override fun parse(yaml: YamlNode): Result<S3ServiceConfiguration> {
    val name =
        when (val name = name.parse(yaml)) {
          is Error<*> -> return Error(name.error)
          is Success<String> -> name.data
        }

    val b =
        when (val result = buckets.parse(yaml)) {
          is Error<List<S3ServiceBucketConfiguration>> -> return Error(result.error)
          is Success<List<S3ServiceBucketConfiguration>> -> result.data
        }

    return Success(S3ServiceConfiguration(name, b))
  }
}
