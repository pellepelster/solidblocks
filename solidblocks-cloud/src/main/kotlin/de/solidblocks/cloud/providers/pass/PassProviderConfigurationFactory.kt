package de.solidblocks.cloud.providers.pass

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.Keyword
import de.solidblocks.cloud.configuration.KeywordHelp
import de.solidblocks.cloud.configuration.StringKeywordOptional
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.configuration.StringConstraints.Companion.NONE
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.providers.DEFAULT_NAME
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.getOptionalString

class PassProviderConfigurationFactory :
    PolymorphicConfigurationFactory<PassProviderConfiguration>() {

    val passwordStoreDir =
        StringKeywordOptional(
            "password_store_dir",
            NONE,
            KeywordHelp(
                "Storage path for the password store, if not set the default or the setting from the `PASSWORD_STORE_DIR` environment variable will be used.",
            ),
        )


    override val help = ConfigurationHelp(
        "Pass",
        "Stores secrets in the [pass](https://www.passwordstore.org/) secret manager. To ensure that the store is setup correctly a temporary secret will be created and deleted during the configuration validation phase."
    )

    override val keywords = listOf<Keyword<*>>(passwordStoreDir)

    override fun parse(yaml: YamlNode): Result<PassProviderConfiguration> {
        val name =
            when (val name = yaml.getOptionalString("name", DEFAULT_NAME)) {
                is Error<String> -> return Error(name.error)
                is Success<String> -> name.data
            }

        return Success(PassProviderConfiguration(name))
    }
}
