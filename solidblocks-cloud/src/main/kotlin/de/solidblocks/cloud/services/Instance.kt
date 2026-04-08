package de.solidblocks.cloud.services

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.Keyword
import de.solidblocks.cloud.configuration.NumberConstraints.Companion.VOLUME_SIZE
import de.solidblocks.cloud.configuration.NumberKeywordOptionalWithDefault
import de.solidblocks.cloud.configuration.StringKeywordOptionalWithDefault
import de.solidblocks.cloud.providers.hetzner.HETZNER_INSTANCE_TYPE
import de.solidblocks.cloud.providers.hetzner.HETZNER_LOCATIONS
import de.solidblocks.cloud.providers.hetzner.HetznerProviderRuntime
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType

object InstanceConfigurationFactory {

  val SERVICE_DATA_VOLUME_SIZE_KEYWORD =
      NumberKeywordOptionalWithDefault(
          "data_size",
          VOLUME_SIZE,
          KeywordHelp(
              "Size in GB for the data volume keeping all data needed for this service.",
          ),
          16,
      )

  val HETZNER_LOCATION_KEYWORD =
      StringKeywordOptionalWithDefault(
          "hetzner_location",
          HETZNER_LOCATIONS,
          HETZNER_LOCATIONS.options.first(),
          KeywordHelp(
              "Hetzner location for created infrastructure resources",
          ),
      )

  val HETZNER_INSTANCE_TYPE_KEYWORD =
      StringKeywordOptionalWithDefault(
          "hetzner_instance_type",
          HETZNER_INSTANCE_TYPE,
          HETZNER_INSTANCE_TYPE.options.first(),
          KeywordHelp(
              "Hetzner instance size for virtual machines",
          ),
      )

  val keywords =
      listOf<Keyword<*>>(
          SERVICE_DATA_VOLUME_SIZE_KEYWORD,
          HETZNER_LOCATION_KEYWORD,
          HETZNER_INSTANCE_TYPE_KEYWORD,
      )

  fun parse(yaml: YamlNode): Result<InstanceConfig> {
    val volumeSize =
        when (val result = SERVICE_DATA_VOLUME_SIZE_KEYWORD.parse(yaml)) {
          is Error<Int> -> return Error(result.error)
          is Success<Int> -> result.data
        }

    val hetznerLocation =
        when (val result = HETZNER_LOCATION_KEYWORD.parse(yaml)) {
          is Error<*> -> return Error(result.error)
          is Success<String> -> result.data
        }

    val hetznerInstanceType =
        when (val result = HETZNER_INSTANCE_TYPE_KEYWORD.parse(yaml)) {
          is Error<*> -> return Error(result.error)
          is Success<String> -> result.data
        }

    return Success(
        InstanceConfig(
            volumeSize,
            HetznerLocation.valueOf(hetznerLocation),
            HetznerServerType.valueOf(hetznerInstanceType),
        ),
    )
  }
}

data class InstanceConfig(
    val volumeSize: Int,
    val hetznerLocation: HetznerLocation?,
    val hetznerInstanceType: HetznerServerType?,
)

data class InstanceRuntime(
    val volumeSize: Int,
    val hetznerLocation: HetznerLocation?,
    val hetznerInstanceType: HetznerServerType?,
) {
  companion object {
    fun fromConfig(config: InstanceConfig) =
        InstanceRuntime(config.volumeSize, config.hetznerLocation, config.hetznerInstanceType)
  }

  fun locationWithDefault(runtime: HetznerProviderRuntime) =
      hetznerLocation ?: runtime.defaultLocation1
}
