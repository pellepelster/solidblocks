package de.solidblocks.cloud.services.s3.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.ConfigurationFactory
import de.solidblocks.cloud.configuration.KeywordHelp
import de.solidblocks.cloud.configuration.SimpleKeyword
import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success

class S3ServiceBucketConfigurationFactory : ConfigurationFactory<S3ServiceBucket> {

  val name =
      StringKeyword(
          "name",
          KeywordHelp(
              "TODO",
              "TODO",
          ),
      )

  /*
  val publicAccess = OptionalBooleanKeyword(
      "publicAccess", KeywordHelp(
          "TODO", "TODO"
      )
  )
   */

  override val help: ConfigurationHelp
    get() = TODO("Not yet implemented")

  override val keywords = listOf<SimpleKeyword<*>>(name)

  override fun parse(yaml: YamlNode): Result<S3ServiceBucket> {
    val name =
        when (val result = name.parse(yaml)) {
          is Error<*> -> return Error(result.error)
          is Success<String> -> result.data
        }

    /*
    val publicAccess = when (val result = publicAccess.parse(yaml)) {
        is Error<*> -> return Error(result.error)
        is Success<Boolean?> -> result.data
    }
     */

    return Success(S3ServiceBucket(name, true)) // TODO
  }
}
