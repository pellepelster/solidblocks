package de.solidblocks.cloud.providers.hetzner

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.KeywordHelp
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.providers.DEFAULT_NAME
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success

class HetznerProviderConfigurationFactory :
    PolymorphicConfigurationFactory<HetznerProviderConfiguration>() {

    val name =
        StringKeyword(
            "name",
            KeywordHelp(
                "TODO",
                "Name for the provider, can be omitted if only one provider of this specific type is configured",
            ),
        ).optional(DEFAULT_NAME)

    val defaultLocation =
        StringKeyword(
            "default-location",
            KeywordHelp(
                "TODO",
                "Default location for created infrastructure resources",
            ),
        ).optional("nbg1")

    val defaultInstanceType =
        StringKeyword(
            "default-instance-type",
            KeywordHelp(
                "TODO",
                "Default instance size for virtual machines",
            ),
        ).optional("cx23")

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
