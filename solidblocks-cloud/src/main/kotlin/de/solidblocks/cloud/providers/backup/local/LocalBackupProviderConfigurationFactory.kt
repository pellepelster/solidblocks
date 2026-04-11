package de.solidblocks.cloud.providers.backup.local

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.services.PROVIDER_NAME_KEYWORD
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success

class LocalBackupProviderConfigurationFactory : PolymorphicConfigurationFactory<LocalBackupProviderConfiguration>() {

    override val help: ConfigurationHelp
        get() = ConfigurationHelp(
            "Backup Local",
            "Provides backup of cloud data to locally attached disk. This provider is always automatically added",
        )

    override val keywords = listOf(PROVIDER_NAME_KEYWORD)

    override fun parse(yaml: YamlNode): Result<LocalBackupProviderConfiguration> {
        val name =
            when (val result = PROVIDER_NAME_KEYWORD.parse(yaml)) {
                is Error<String> -> return Error(result.error)
                is Success<String> -> result.data
            }

        return Success(LocalBackupProviderConfiguration(name))
    }
}
