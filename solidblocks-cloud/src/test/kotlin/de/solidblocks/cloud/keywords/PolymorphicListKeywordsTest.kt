package de.solidblocks.cloud.keywords

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.TEST_KEYWORD_HELP
import de.solidblocks.cloud.configuration.Keyword
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.configuration.PolymorphicListKeyword
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.yamlParse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class PolymorphicListKeywordsTest {

    interface BaseType

    class Type1 : BaseType

    class Type2 : BaseType

    class Type1Factory : PolymorphicConfigurationFactory<Type1>() {
        override val help: ConfigurationHelp
            get() = TODO("Not yet implemented")

        override val keywords: List<Keyword<*>>
            get() = TODO("Not yet implemented")

        override fun parse(yaml: YamlNode): Result<Type1> = Success(Type1())
    }

    class Type2Factory : PolymorphicConfigurationFactory<Type2>() {
        override val help: ConfigurationHelp
            get() = TODO("Not yet implemented")

        override val keywords: List<Keyword<*>>
            get() = TODO("Not yet implemented")

        override fun parse(yaml: YamlNode): Result<Type2> = Success(Type2())
    }

    private fun keyword() = PolymorphicListKeyword<BaseType>(
        "list1",
        mapOf("type1" to Type1Factory(), "type2" to Type2Factory()),
        TEST_KEYWORD_HELP,
    )

    @Test
    fun `polymorphic list keyword`() {
        val yaml =
            yamlParse(
                """
                list1:
                    - type: type1
                    - type: type2
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val list = keyword().parse(yaml.data).shouldBeTypeOf<Success<List<BaseType>>>().data

        list shouldHaveSize 2
        list[0].shouldBeTypeOf<Type1>()
        list[1].shouldBeTypeOf<Type2>()
    }

    @Test
    fun `polymorphic list keyword missing key returns empty list`() {
        val yaml =
            yamlParse(
                """
                list2:
                    - type: type1
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        keyword().parse(yaml.data).shouldBeTypeOf<Success<List<BaseType>>>().data shouldHaveSize 0
    }

    @Test
    fun `polymorphic list keyword unknown type returns error`() {
        val yaml =
            yamlParse(
                """
                list1:
                    - type: type123
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        keyword().parse(yaml.data).shouldBeTypeOf<Error<List<BaseType>>>().error shouldBe
            "unknown type 'type123', possible types are 'type1', 'type2' at line 2 column 7"
    }

    @Test
    fun `polymorphic list keyword missing type returns error`() {
        val yaml =
            yamlParse(
                """
                list1:
                    - name: foo-bar
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        keyword().parse(yaml.data).shouldBeTypeOf<Error<List<BaseType>>>().error shouldBe
            "key 'type' not found at line 2 column 7"
    }

    @Test
    fun `polymorphic list keyword returns error when key is not a list`() {
        val yaml =
            yamlParse(
                """
                list1:
                    type: type1
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        keyword().parse(yaml.data).shouldBeTypeOf<Error<List<BaseType>>>().error shouldBe
            "key 'list1' should be a list line 1 column 1"
    }
}
