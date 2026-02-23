package de.solidblocks.cloud.providers.sshkey

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.KeywordHelp
import de.solidblocks.cloud.configuration.OptionalStringKeyword
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.providers.DEFAULT_NAME
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.getOptionalString

class LocalSSHKeyProviderConfigurationFactory :
    PolymorphicConfigurationFactory<LocalSSHKeyProviderConfiguration>() {

    val privateKey =
        OptionalStringKeyword(
            "private_key",
            KeywordHelp(
                "",
                "path to the private key, if not set, the default SSH key paths will be tried.",
            ),
        )

    override val help: ConfigurationHelp
        get() = TODO("Not yet implemented")

    override val keywords = listOf(privateKey)

    override fun parse(yaml: YamlNode): Result<LocalSSHKeyProviderConfiguration> {
        val name =
            when (val name = yaml.getOptionalString("name", DEFAULT_NAME)) {
                is Error<String> -> return Error(name.error)
                is Success<String> -> name.data
            }

        val privateKey =
            when (val name = privateKey.parse(yaml)) {
                is Error<String?> -> return Error(name.error)
                is Success<String?> -> name.data
            }

        return Success(LocalSSHKeyProviderConfiguration(name, privateKey))
    }
}
