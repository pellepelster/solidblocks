package de.solidblocks.cloud.providers.hetzner

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.KeywordHelp
import de.solidblocks.cloud.configuration.StringKeywordOptionalWithDefault
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.configuration.StringConstraints.Companion.NONE
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.providers.DEFAULT_NAME
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success

class HetznerProviderConfigurationFactory :
    PolymorphicConfigurationFactory<HetznerProviderConfiguration>() {

    val name =
        StringKeywordOptionalWithDefault(
            "name",
            NONE,
            DEFAULT_NAME,
            KeywordHelp(
                "Name for the provider, can be omitted if only one provider of this specific type is configured",
            ),
        )

    val defaultLocation =
        StringKeywordOptionalWithDefault(
            "default-location",
            HETZNER_LOCATIONS,
            HETZNER_LOCATIONS.options.first(),
            KeywordHelp(
                "Default location for created infrastructure resources",
            ),
        )

    val defaultInstanceType =
        StringKeywordOptionalWithDefault(
            "default-instance-type",
            HETZNER_INSTANCE_TYPE,
            HETZNER_INSTANCE_TYPE.options.first(),
            KeywordHelp(
                "Default instance size for virtual machines",
            ),
        )

    override val help = ConfigurationHelp("Hetzner", "Provides Hetzner Cloud based infrastructure resources. An API key with read/write access must be provided via the environment variable `HCLOUD_TOKEN`.")

    override val keywords = listOf(name, defaultLocation, defaultInstanceType)

    override fun parse(yaml: YamlNode): Result<HetznerProviderConfiguration> {

        val name = when (val result = name.parse(yaml)) {
            is Error<String> -> return Error(result.error)
            is Success<String> -> result.data
        }

        val defaultLocation = when (val result = defaultLocation.parse(yaml)) {
            is Error<*> -> return Error(result.error)
            is Success<String> -> result.data
        }

        val defaultInstanceType = when (val result = defaultInstanceType.parse(yaml)) {
            is Error<*> -> return Error(result.error)
            is Success<String> -> result.data
        }

        return Success(HetznerProviderConfiguration(name, defaultLocation, defaultInstanceType))
    }
}
