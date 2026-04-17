package de.solidblocks.cloud

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.ListKeyword
import de.solidblocks.cloud.configuration.StringListKeyword
import de.solidblocks.cloud.mocks.Test1Configuration
import de.solidblocks.cloud.mocks.Test1ConfigurationFactory
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
    fun testObjectListKeyword() {
        val yaml =
            yamlParse(
                """
                list1:
                    - name: foo-bar
                    - name: yolo2000
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
    fun testStringListKeyword() {
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
    fun testStringListKeywordEmpty() {
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
            "expected a string at line 2 colum 6"
    }
}
