package de.solidblocks.cli.mock

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.ConfigurationFactory
import de.solidblocks.cloud.configuration.SimpleKeyword
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.getBoolean
import de.solidblocks.cloud.utils.getNonNullOrEmptyString
import de.solidblocks.cloud.utils.logMessage

data class Test1Configuration(val name: String, val flag1: Boolean)

class Test1ConfigurationFactory : ConfigurationFactory<Test1Configuration> {

  override val help: ConfigurationHelp
    get() = TODO("Not yet implemented")

  override val keywords = emptyList<SimpleKeyword<*>>()

  override fun parse(yaml: YamlNode): Result<Test1Configuration> {
    val name =
        when (val string = yaml.getNonNullOrEmptyString("name")) {
          is Error<String> -> return Error(string.error)
          is Success<String> -> string.data
        }

    if (name == "forbidden") {
      return Error("name 'forbidden' is not allowed at ${yaml.location.logMessage()}")
    }

    val flag1 =
        when (val flag = yaml.getBoolean("flag1", false)) {
          is Error<Boolean> -> return Error(flag.error)
          is Success<Boolean> -> flag.data
        }

    return Success(Test1Configuration(name, flag1))
  }
}
