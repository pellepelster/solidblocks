package de.solidblocks.cloud.services

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.Keyword
import de.solidblocks.cloud.configuration.NumberConstraints.Companion.VOLUME_SIZE
import de.solidblocks.cloud.configuration.NumberKeyword
import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.configuration.constraints
import de.solidblocks.cloud.configuration.default
import de.solidblocks.cloud.providers.hetzner.HETZNER_INSTANCE_TYPE
import de.solidblocks.cloud.providers.hetzner.HETZNER_LOCATIONS
import de.solidblocks.cloud.providers.hetzner.HetznerProviderRuntime
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.result
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType

data class InstanceRuntime(val volumeSize: Int, val hetznerLocation: HetznerLocation?, val hetznerInstanceType: HetznerServerType?) {
    fun locationWithDefault(runtime: HetznerProviderRuntime) = hetznerLocation ?: runtime.defaultLocation1
}

fun InstanceConfig.toRuntime() = InstanceRuntime(this.volumeSize, this.hetznerLocation, this.hetznerInstanceType)

object InstanceConfigurationFactory {

    val SERVICE_DATA_VOLUME_SIZE_KEYWORD =
        NumberKeyword(
            "data_size",
            KeywordHelp(
                "Size in GB for the data volume keeping all data needed for this service.",
            ),
        ).constraints(VOLUME_SIZE).default(16)

    val HETZNER_LOCATION_KEYWORD =
        StringKeyword(
            "hetzner_location",
            KeywordHelp(
                "Hetzner location for created infrastructure resources, if not set the default from the Hetzner provider configuration is used.",
            ),
        ).constraints(HETZNER_LOCATIONS).default(HETZNER_LOCATIONS.options.first())

    val HETZNER_INSTANCE_TYPE_KEYWORD =
        StringKeyword(
            "hetzner_instance_type",
            KeywordHelp(
                "Hetzner instance size for virtual machines, if not set the default from the Hetzner provider configuration is used.",
            ),
        ).constraints(HETZNER_INSTANCE_TYPE).default(HETZNER_INSTANCE_TYPE.options.first())

    val keywords =
        listOf<Keyword<*>>(
            SERVICE_DATA_VOLUME_SIZE_KEYWORD,
            HETZNER_LOCATION_KEYWORD,
            HETZNER_INSTANCE_TYPE_KEYWORD,
        )

    fun YamlNode.parseInstanceConfig(): Result<InstanceConfig> {
        val yaml = this
        return result {
            InstanceConfig(
                SERVICE_DATA_VOLUME_SIZE_KEYWORD.parse(yaml).bind(),
                HetznerLocation.valueOf(HETZNER_LOCATION_KEYWORD.parse(yaml).bind()),
                HetznerServerType.valueOf(HETZNER_INSTANCE_TYPE_KEYWORD.parse(yaml).bind()),
            )
        }
    }
}
