package de.solidblocks.cloud.configuration

import de.solidblocks.cloud.utils.*
import java.io.File

class ConfigurationParser<T>(val factory: ConfigurationFactory<T>) {

  fun parse(configurationFile: File): Result<T> {
    if (!configurationFile.exists() || configurationFile.isDirectory) {
      return Error("file '${configurationFile.absolutePath}' does not exist or is not a file")
    }

    return parse(configurationFile.readText())
  }

  fun parse(configuration: String): Result<T> {
    val yaml =
        when (val result = yamlParse(configuration)) {
          is Error -> return Error("failed to parse cloud configuration (${result.error})")
          is YamlEmpty -> return Error("cloud configuration was empty")
          is Success -> result.data
        }
    return factory.parse(yaml)
  }
}
