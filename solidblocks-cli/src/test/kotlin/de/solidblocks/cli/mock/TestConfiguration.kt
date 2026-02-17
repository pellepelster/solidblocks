package de.solidblocks.cli.mock

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.ConfigurationFactory
import de.solidblocks.cloud.configuration.KeywordHelp
import de.solidblocks.cloud.configuration.ListKeyword
import de.solidblocks.cloud.configuration.ObjectKeyword
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success

data class TestConfiguration(val test1: List<Test1Configuration>, val test2: Test2Configuration)

class TestConfigurationFactory : ConfigurationFactory<TestConfiguration> {

  override val help: ConfigurationHelp
    get() = TODO("Not yet implemented")

  val test1 = ListKeyword("test1", Test1ConfigurationFactory(), KeywordHelp("", ""))

  /*
  val polymorphicList1 =
      PolymorphicListKeyword("polymorphic_list1", listOf(Test1ConfigurationFactory()), KeywordHelp("", ""))
   */

  val test2 = ObjectKeyword("test2", Test2ConfigurationFactory(), KeywordHelp("", ""))

  override val keywords = listOf(test1, test2)

  override fun parse(yaml: YamlNode): Result<TestConfiguration> {
    val test1 =
        when (val test1 = this@TestConfigurationFactory.test1.parse(yaml)) {
          is Error<List<Test1Configuration>> -> return Error(test1.error)
          is Success<List<Test1Configuration>> -> test1.data
        }

    val test2 =
        when (val test2 = this@TestConfigurationFactory.test2.parse(yaml)) {
          is Error<Test2Configuration> -> return Error(test2.error)
          is Success<Test2Configuration> -> test2.data
        }

    return Success(
        TestConfiguration(
            test1,
            test2,
        ),
    )
  }
}
