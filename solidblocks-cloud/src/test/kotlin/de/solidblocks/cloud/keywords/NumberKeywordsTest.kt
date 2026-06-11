package de.solidblocks.cloud.keywords

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.TEST_KEYWORD_HELP
import de.solidblocks.cloud.configuration.NumberConstraints
import de.solidblocks.cloud.configuration.NumberKeyword
import de.solidblocks.cloud.configuration.constraints
import de.solidblocks.cloud.configuration.default
import de.solidblocks.cloud.configuration.optional
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.yamlParse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class NumberKeywordsTest {

    @Test
    fun `number keyword`() {
        val yaml =
            yamlParse(
                """
                number1: 123
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = NumberKeyword("number1", TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Int>>().data shouldBe 123
    }

    @Test
    fun `number keyword missing key returns error`() {
        val yaml =
            yamlParse(
                """
                number2: 123
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = NumberKeyword("number1", TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Error<Int>>().error shouldBe
            "key 'number1' not found at line 1 column 1"
    }

    @Test
    fun `number keyword invalid value returns error`() {
        val yaml =
            yamlParse(
                """
                number1: abc
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = NumberKeyword("number1", TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Error<Int>>().error shouldBe
            "expected number but got 'abc' at line 1 column 1"
    }

    @Test
    fun `number keyword optional`() {
        val yaml =
            yamlParse(
                """
                number2: 123
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = NumberKeyword("number1", TEST_KEYWORD_HELP).optional()
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Int?>>().data shouldBe null
    }

    @Test
    fun `number keyword optional returns value when key present`() {
        val yaml =
            yamlParse(
                """
                number1: 123
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = NumberKeyword("number1", TEST_KEYWORD_HELP).optional()
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Int?>>().data shouldBe 123
    }

    @Test
    fun `number keyword with default`() {
        val yaml =
            yamlParse(
                """
                number2: 123
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = NumberKeyword("number1", TEST_KEYWORD_HELP).default(124)
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Int>>().data shouldBe 124
    }

    @Test
    fun `number keyword with default returns value when key present`() {
        val yaml =
            yamlParse(
                """
                number1: 123
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = NumberKeyword("number1", TEST_KEYWORD_HELP).default(124)
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Int>>().data shouldBe 123
    }

    @Test
    fun `number keyword min`() {
        val yaml =
            yamlParse(
                """
                number1: 21
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = NumberKeyword("number1", TEST_KEYWORD_HELP).constraints(NumberConstraints(min = 22))
        keyword.parse(yaml.data).shouldBeTypeOf<Error<Int>>().error shouldBe
            "'number1' may not be smaller than 22 at line 1 column 1"
    }

    @Test
    fun `number keyword max`() {
        val yaml =
            yamlParse(
                """
                number1: 21
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = NumberKeyword("number1", TEST_KEYWORD_HELP).constraints(NumberConstraints(max = 12))
        keyword.parse(yaml.data).shouldBeTypeOf<Error<Int>>().error shouldBe
            "'number1' may not be larger than 12 at line 1 column 1"
    }

    @Test
    fun `number keyword optional applies constraints`() {
        val yaml =
            yamlParse(
                """
                number1: 21
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            NumberKeyword("number1", TEST_KEYWORD_HELP).constraints(NumberConstraints(max = 12)).optional()
        keyword.parse(yaml.data).shouldBeTypeOf<Error<Int?>>().error shouldBe
            "'number1' may not be larger than 12 at line 1 column 1"
    }

    @Test
    fun `number keyword with default applies constraints`() {
        val yaml =
            yamlParse(
                """
                number1: 21
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            NumberKeyword("number1", TEST_KEYWORD_HELP).constraints(NumberConstraints(max = 12)).default(10)
        keyword.parse(yaml.data).shouldBeTypeOf<Error<Int>>().error shouldBe
            "'number1' may not be larger than 12 at line 1 column 1"
    }

    @Test
    fun `number keyword exposes metadata`() {
        val required = NumberKeyword("number1", TEST_KEYWORD_HELP)
        required.optional shouldBe false
        required.default shouldBe null

        val optional = NumberKeyword("number1", TEST_KEYWORD_HELP).optional()
        optional.optional shouldBe true
        optional.default shouldBe null

        val withDefault = NumberKeyword("number1", TEST_KEYWORD_HELP).default(124)
        withDefault.optional shouldBe true
        withDefault.default shouldBe 124

        val withConstraints =
            NumberKeyword("number1", TEST_KEYWORD_HELP).constraints(NumberConstraints.VOLUME_SIZE)
        withConstraints.constraints shouldBe NumberConstraints.VOLUME_SIZE
        withConstraints.optional shouldBe false
    }

    @Test
    fun `number keyword null value returns error`() {
        val yaml =
            yamlParse(
                """
                number1: null
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = NumberKeyword("number1", TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Error<Int>>().error shouldBe
            "key 'number1' is null at line 1 column 1"
    }

    @Test
    fun `number keyword empty value returns error`() {
        val yaml =
            yamlParse(
                """
                number1: ""
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = NumberKeyword("number1", TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Error<Int>>().error shouldBe
            "key 'number1' is empty at line 1 column 1"
    }

    @Test
    fun `number keyword optional null value returns null`() {
        val yaml =
            yamlParse(
                """
                number1: null
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = NumberKeyword("number1", TEST_KEYWORD_HELP).optional()
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Int?>>().data shouldBe null
    }

    @Test
    fun `number keyword with default null value returns default`() {
        val yaml =
            yamlParse(
                """
                number1: null
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = NumberKeyword("number1", TEST_KEYWORD_HELP).default(124)
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Int>>().data shouldBe 124
    }

    @Test
    fun `number keyword with default empty value returns default`() {
        val yaml =
            yamlParse(
                """
                number1: ""
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = NumberKeyword("number1", TEST_KEYWORD_HELP).default(124)
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Int>>().data shouldBe 124
    }

    @Test
    fun `number keyword non scalar value returns error`() {
        val yaml =
            yamlParse(
                """
                number1:
                  foo: bar
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = NumberKeyword("number1", TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Error<Int>>().error shouldBe
            "expected string but found '{'foo': 'bar'}'"
    }

    @Test
    fun `number keyword exact min is allowed`() {
        val yaml =
            yamlParse(
                """
                number1: 16
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = NumberKeyword("number1", TEST_KEYWORD_HELP).constraints(NumberConstraints(max = 1024, min = 16))
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Int>>().data shouldBe 16
    }

    @Test
    fun `number keyword exact max is allowed`() {
        val yaml =
            yamlParse(
                """
                number1: 1024
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = NumberKeyword("number1", TEST_KEYWORD_HELP).constraints(NumberConstraints(max = 1024, min = 16))
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Int>>().data shouldBe 1024
    }

    @Test
    fun `number keyword negative value`() {
        val yaml =
            yamlParse(
                """
                number1: -42
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = NumberKeyword("number1", TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Int>>().data shouldBe -42
    }

    @Test
    fun `number keyword decimal value returns error`() {
        val yaml =
            yamlParse(
                """
                number1: 1.5
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = NumberKeyword("number1", TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Error<Int>>().error shouldBe
            "expected number but got '1.5' at line 1 column 1"
    }

    @Test
    fun `number keyword quoted value`() {
        val yaml =
            yamlParse(
                """
                number1: "123"
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = NumberKeyword("number1", TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Int>>().data shouldBe 123
    }

    @Test
    fun `number keyword constraint violating default returns error when key missing`() {
        val yaml =
            yamlParse(
                """
                number2: 1
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            NumberKeyword("number1", TEST_KEYWORD_HELP).constraints(NumberConstraints(max = 12)).default(20)
        keyword.parse(yaml.data).shouldBeTypeOf<Error<Int>>().error shouldBe
            "'number1' may not be larger than 12 at line 1 column 1"
    }
}
