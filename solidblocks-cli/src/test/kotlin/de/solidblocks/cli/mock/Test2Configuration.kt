package de.solidblocks.cli.mock

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.ConfigurationFactory
import de.solidblocks.cloud.configuration.SimpleKeyword
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.getNumber

data class Test2Configuration(val number1: Number)

class Test2ConfigurationFactory : ConfigurationFactory<Test2Configuration> {

  override val help: ConfigurationHelp
    get() = TODO("Not yet implemented")

  override val keywords = emptyList<SimpleKeyword<*>>()

  override fun parse(yaml: YamlNode): Result<Test2Configuration> {
    val number1 =
        when (val number = yaml.getNumber("number1", 12)) {
          is Error<Number> -> return Error(number.error)
          is Success<Number> -> number.data
        }

    return Success(Test2Configuration(number1))
  }
}
