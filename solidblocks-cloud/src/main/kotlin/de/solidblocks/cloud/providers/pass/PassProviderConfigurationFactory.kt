package de.solidblocks.cloud.providers.pass

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.Keyword
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.providers.DEFAULT_NAME
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.getOptionalString

class PassProviderConfigurationFactory :
    PolymorphicConfigurationFactory<PassProviderConfiguration>() {

  override val help: ConfigurationHelp
    get() = TODO("Not yet implemented")

  override val keywords: List<Keyword<*>>
    get() = TODO("Not yet implemented")

  override fun parse(yaml: YamlNode): Result<PassProviderConfiguration> {
    val name =
        when (val name = yaml.getOptionalString("name", DEFAULT_NAME)) {
          is Error<String> -> return Error(name.error)
          is Success<String> -> name.data
        }

    return Success(PassProviderConfiguration(name))
  }
}
