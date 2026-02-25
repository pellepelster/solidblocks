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
                "TODO",
            ),
        ).optional(DEFAULT_NAME)

    val defaultLocation =
        StringKeyword(
            "default-location",
            KeywordHelp(
                "TODO",
                "TODO",
            ),
        ).optional("nbg1")

    val defaultInstanceType =
        StringKeyword(
            "default-instance-type",
            KeywordHelp(
                "TODO",
                "TODO",
            ),
        ).optional("cx23")

    override val help: ConfigurationHelp
        get() = TODO("Not yet implemented")

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
