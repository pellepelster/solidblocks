package de.solidblocks.cloud.mocks

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.TEST_KEYWORD_HELP
import de.solidblocks.cloud.configuration.ConfigurationFactory
import de.solidblocks.cloud.configuration.SimpleKeyword
import de.solidblocks.cloud.configuration.StringConstraints.Companion.NONE
import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success

data class Test1Configuration(val name: String)

class Test1ConfigurationFactory : ConfigurationFactory<Test1Configuration> {

    val name =
        StringKeyword(
            "name",
            NONE,
            TEST_KEYWORD_HELP,
        )

    override val help = ConfigurationHelp("TODO", "TODO")

    override val keywords = emptyList<SimpleKeyword<*>>()

    override fun parse(yaml: YamlNode): Result<Test1Configuration> {
        val name =
            when (val string = name.parse(yaml)) {
                is Error<String> -> return Error(string.error)
                is Success<String> -> string.data
            }

        return Success(Test1Configuration(name))
    }
}
