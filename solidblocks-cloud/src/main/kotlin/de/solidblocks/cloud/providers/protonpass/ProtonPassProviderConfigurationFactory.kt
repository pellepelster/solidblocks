package de.solidblocks.cloud.providers.protonpass

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.Keyword
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.configuration.StringConstraints.Companion.NONE
import de.solidblocks.cloud.configuration.StringKeywordOptional
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.services.PROVIDER_NAME_KEYWORD
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success

class ProtonPassProviderConfigurationFactory : PolymorphicConfigurationFactory<ProtonPassProviderConfiguration>() {

    val vaultName =
        StringKeywordOptional(
            "vault_name",
            NONE,
            KeywordHelp(
                "Name of the Proton Pass vault to store secrets in, if not set the cloud name will be used.",
            ),
        )

    override val help =
        ConfigurationHelp(
            "Proton Pass",
            "Stores secrets in a [Proton Pass](https://proton.me/pass) vault using the `pass-cli` command line tool. To ensure that the vault is setup correctly a temporary secret will be created and deleted during the configuration validation phase. The validation can be skipped by setting the environment variable 'BLCKS_PROTONPASS_PROVIDER_SKIP_VALIDATION'",
        )

    override val keywords = listOf<Keyword<*>>(PROVIDER_NAME_KEYWORD, vaultName)

    override fun parse(yaml: YamlNode): Result<ProtonPassProviderConfiguration> {
        val name =
            when (val name = PROVIDER_NAME_KEYWORD.parse(yaml)) {
                is Error<String> -> return Error(name.error)
                is Success<String> -> name.data
            }

        val vaultName =
            when (val vaultName = vaultName.parse(yaml)) {
                is Error<String?> -> return Error(vaultName.error)
                is Success<String?> -> vaultName.data
            }

        return Success(ProtonPassProviderConfiguration(name, vaultName))
    }
}
