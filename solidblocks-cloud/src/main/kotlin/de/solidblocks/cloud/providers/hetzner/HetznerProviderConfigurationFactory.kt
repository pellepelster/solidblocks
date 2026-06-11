package de.solidblocks.cloud.providers.hetzner

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.configuration.constraints
import de.solidblocks.cloud.configuration.default
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.services.PROVIDER_NAME_KEYWORD
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.result
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType

class HetznerProviderConfigurationFactory : PolymorphicConfigurationFactory<HetznerProviderConfiguration>() {

    val defaultLocation =
        StringKeyword(
            "default_location",
            KeywordHelp(
                "Default location for created infrastructure resources",
            ),
        ).constraints(HETZNER_LOCATIONS).default(HETZNER_LOCATIONS.options.first())

    val defaultInstanceType =
        StringKeyword(
            "default-instance-type",
            KeywordHelp(
                "Default instance size for virtual machines",
            ),
        ).constraints(HETZNER_INSTANCE_TYPE).default(HETZNER_INSTANCE_TYPE.options.first())

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
