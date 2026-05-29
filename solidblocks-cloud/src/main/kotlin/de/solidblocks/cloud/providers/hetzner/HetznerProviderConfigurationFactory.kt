package de.solidblocks.cloud.providers.hetzner

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.configuration.StringKeywordOptionalWithDefault
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.services.PROVIDER_NAME_KEYWORD
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.result
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType

class HetznerProviderConfigurationFactory : PolymorphicConfigurationFactory<HetznerProviderConfiguration>() {

    val defaultLocation =
        StringKeywordOptionalWithDefault(
            "default_location",
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

    override val help =
        ConfigurationHelp(
            "Hetzner",
            "Provides Hetzner Cloud based infrastructure resources. An API key with read/write access must be provided via the environment variable `HCLOUD_TOKEN`.",
        )

    override val keywords = listOf(PROVIDER_NAME_KEYWORD, defaultLocation, defaultInstanceType)

    override fun parse(yaml: YamlNode): Result<HetznerProviderConfiguration> = result {
        HetznerProviderConfiguration(
            PROVIDER_NAME_KEYWORD.parse(yaml).bind(),
            HetznerLocation.valueOf(defaultLocation.parse(yaml).bind()),
            HetznerServerType.valueOf(defaultInstanceType.parse(yaml).bind()),
        )
    }
}
