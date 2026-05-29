package de.solidblocks.cloud.providers.backup.local

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.services.PROVIDER_NAME_KEYWORD
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.result

class LocalBackupProviderConfigurationFactory : PolymorphicConfigurationFactory<LocalBackupProviderConfiguration>() {

    override val help: ConfigurationHelp
        get() = ConfigurationHelp(
            "Backup Local",
            "Enables use of locally attached disks for service backups.",
        )

    override val keywords = listOf(PROVIDER_NAME_KEYWORD)

    override fun parse(yaml: YamlNode): Result<LocalBackupProviderConfiguration> = result {
        LocalBackupProviderConfiguration(PROVIDER_NAME_KEYWORD.parse(yaml).bind())
    }
}
