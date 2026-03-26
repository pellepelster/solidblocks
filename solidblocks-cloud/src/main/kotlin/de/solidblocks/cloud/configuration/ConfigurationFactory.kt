package de.solidblocks.cloud.configuration

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.utils.Result

interface ConfigurationFactory<T> {
  val help: ConfigurationHelp
  val keywords: List<Keyword<*>>

  fun parse(yaml: YamlNode): Result<T>
}

abstract class PolymorphicConfigurationFactory<T> : ConfigurationFactory<T>
