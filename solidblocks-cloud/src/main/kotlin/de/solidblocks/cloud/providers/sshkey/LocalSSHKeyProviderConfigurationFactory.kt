package de.solidblocks.cloud.providers.sshkey

import com.charleskorn.kaml.YamlNode
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

class LocalSSHKeyProviderConfigurationFactory :
    PolymorphicConfigurationFactory<LocalSSHKeyProviderConfiguration>() {

    companion object {
        val defaultSSHKeyNames =
            listOf(
                "id_rsa",
                "id_ecdsa",
                "id_ecdsa_sk",
                "id_ed25519",
                "id_ed25519_sk",
            )
    }

    val privateKey =
        StringKeywordOptional(
            "private_key",
            NONE,
            KeywordHelp(
                "Path to the private key, if not set, the default SSH key paths will be tried (${
                    defaultSSHKeyNames.joinToString(", ") {
                        "'~/.ssh/$it'"
                    }
                })"
            )
        )

    override val help = ConfigurationHelp("Local SSH", "A provider that loads local file based SSH keys. It supports passwordless PEM as well as OpenSSH encoded private keys.")

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
