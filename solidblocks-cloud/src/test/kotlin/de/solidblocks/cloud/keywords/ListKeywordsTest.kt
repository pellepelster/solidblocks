package de.solidblocks.cloud.keywords

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.TEST_KEYWORD_HELP
import de.solidblocks.cloud.configuration.ListKeyword
import de.solidblocks.cloud.configuration.StringListKeyword
import de.solidblocks.cloud.mocks.Test2Configuration
import de.solidblocks.cloud.mocks.Test2ConfigurationFactory
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.yamlParse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class ListKeywordsTest {

    @Test
    fun `object list keyword`() {
        val yaml =
            yamlParse(
                """
                list1:
                    - name: foo-bar
                      number1: 1
                    - name: yolo2000
                      number1: 2
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            ListKeyword(
                "list1",
                Test2ConfigurationFactory(),
                TEST_KEYWORD_HELP,
            )

        val list = keyword.parse(yaml.data).shouldBeTypeOf<Success<List<Test2Configuration>>>().data

        list shouldHaveSize 2
        list[0].name shouldBe "foo-bar"
        list[1].name shouldBe "yolo2000"
    }

    @Test
    fun `string list keyword`() {
        val yaml =
            yamlParse(
                """
                list1:
                    - foo-bar
                    - yolo2000
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            StringListKeyword(
                "list1",
                TEST_KEYWORD_HELP,
            )

        val list = keyword.parse(yaml.data).shouldBeTypeOf<Success<List<String>>>().data

        list shouldHaveSize 2
        list[0] shouldBe "foo-bar"
        list[1] shouldBe "yolo2000"
    }

    @Test
    fun `string list keyword empty`() {
        val yaml =
            yamlParse(
                """
                list1:
                    - 
                    - yolo2000
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            StringListKeyword(
                "list1",
                TEST_KEYWORD_HELP,
            )

        keyword.parse(yaml.data).shouldBeTypeOf<Error<List<String>>>().error shouldBe
            "expected a string at line 2 column 6"
    }

    @Test
    fun `list keyword missing key returns empty list`() {
        val yaml =
            yamlParse(
                """
                list2:
                    - name: foo-bar
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            ListKeyword(
                "list1",
                Test2ConfigurationFactory(),
                TEST_KEYWORD_HELP,
            )

        keyword.parse(yaml.data).shouldBeTypeOf<Success<List<Test2Configuration>>>().data shouldHaveSize 0
    }

    @Test
    fun `list keyword empty list`() {
        val yaml =
            yamlParse(
                """
                list1: []
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            ListKeyword(
                "list1",
                Test2ConfigurationFactory(),
                TEST_KEYWORD_HELP,
            )

        keyword.parse(yaml.data).shouldBeTypeOf<Success<List<Test2Configuration>>>().data shouldHaveSize 0
    }

    @Test
    fun `list keyword returns error when key is not a list`() {
        val yaml =
            yamlParse(
                """
                list1:
                    name: foo-bar
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            ListKeyword(
                "list1",
                Test2ConfigurationFactory(),
                TEST_KEYWORD_HELP,
            )

        keyword.parse(yaml.data).shouldBeTypeOf<Error<List<Test2Configuration>>>().error shouldBe
            "key 'list1' should be a list line 1 column 1"
    }

    @Test
    fun `object list keyword aggregates item errors`() {
        val yaml =
            yamlParse(
                """
                list1:
                    - number1: abc
                    - number1: def
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            ListKeyword(
                "list1",
                Test2ConfigurationFactory(),
                TEST_KEYWORD_HELP,
            )

        keyword.parse(yaml.data).shouldBeTypeOf<Error<List<Test2Configuration>>>().error shouldBe
            "expected number but got 'abc' at line 2 column 7, expected number but got 'def' at line 3 column 7"
    }

    @Test
    fun `string list keyword returns error for non scalar item`() {
        val yaml =
            yamlParse(
                """
                list1:
                    - foo-bar
                    - bar: baz
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            StringListKeyword(
                "list1",
                TEST_KEYWORD_HELP,
            )

        keyword.parse(yaml.data).shouldBeTypeOf<Error<List<String>>>().error shouldBe
            "expected a string at line 3 column 7"
    }
}
