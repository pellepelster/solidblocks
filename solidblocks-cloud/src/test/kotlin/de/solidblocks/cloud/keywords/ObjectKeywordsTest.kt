package de.solidblocks.cloud.keywords

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.TEST_KEYWORD_HELP
import de.solidblocks.cloud.configuration.ObjectKeyword
import de.solidblocks.cloud.mocks.Test2Configuration
import de.solidblocks.cloud.mocks.Test2ConfigurationFactory
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.yamlParse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class ObjectKeywordsTest {

    @Test
    fun `object keyword`() {
        val yaml =
            yamlParse(
                """
                object1:
                    name: foo-bar
                    number1: 1
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = ObjectKeyword("object1", Test2ConfigurationFactory(), TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Test2Configuration>>().data shouldBe
            Test2Configuration("foo-bar", 1)
    }

    @Test
    fun `object keyword missing key parses empty map`() {
        val yaml =
            yamlParse(
                """
                object2:
                    name: foo-bar
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = ObjectKeyword("object1", Test2ConfigurationFactory(), TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Test2Configuration>>().data shouldBe
            Test2Configuration("foo-bar", 12)
    }

    @Test
    fun `object keyword returns error when key is a list`() {
        val yaml =
            yamlParse(
                """
                object1:
                    - name: foo-bar
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = ObjectKeyword("object1", Test2ConfigurationFactory(), TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Error<Test2Configuration>>().error shouldBe
            "key 'object1' should be a map line 1 column 1"
    }

    @Test
    fun `object keyword propagates factory errors`() {
        val yaml =
            yamlParse(
                """
                object1:
                    number1: abc
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = ObjectKeyword("object1", Test2ConfigurationFactory(), TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Error<Test2Configuration>>().error shouldBe
            "expected number but got 'abc' at line 2 column 5"
    }
}
