package de.solidblocks.cloud.mocks

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.api.Error
import de.solidblocks.cloud.api.Result
import de.solidblocks.cloud.api.Success
import de.solidblocks.cloud.configuration.ConfigurationFactory
import de.solidblocks.cloud.configuration.SimpleKeyword
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.utils.YamlEmpty
import de.solidblocks.cloud.utils.YamlError
import de.solidblocks.cloud.utils.YamlSuccess
import de.solidblocks.cloud.utils.getNumber
import de.solidblocks.cloud.utils.getOptionalString

data class Test2Configuration(val name: String, val number1: Number)

class Test2ConfigurationFactory : ConfigurationFactory<Test2Configuration> {
    override val help: ConfigurationHelp
        get() = TODO("Not yet implemented")

    override val keywords = emptyList<SimpleKeyword<*>>()

    override fun parse(yaml: YamlNode): Result<Test2Configuration> {
        val name =
            when (val result = yaml.getOptionalString("name", "foo-bar")) {
                is YamlEmpty<String> -> "foo-bar"
                is YamlError<String> -> return Error(result.error)
                is YamlSuccess<String> -> result.data
            }

        val number =
            when (val result = yaml.getNumber("number1", 12)) {
                is YamlEmpty<Number> -> return Error(result.message)
                is YamlError<Number> -> return Error(result.error)
                is YamlSuccess<Number> -> result.data
            }

        return Success(Test2Configuration(name, number))
    }
}
