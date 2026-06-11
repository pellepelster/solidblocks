package de.solidblocks.cloud.keywords

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.TEST_KEYWORD_HELP
import de.solidblocks.cloud.configuration.StringConstraints
import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.configuration.constraints
import de.solidblocks.cloud.configuration.default
import de.solidblocks.cloud.configuration.optional
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.yamlParse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class StringKeywordsTest {

    @Test
    fun `string keyword`() {
        val yaml =
            yamlParse(
                """
                string1: foo-bar
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = StringKeyword("string1", TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Success<String>>().data shouldBe "foo-bar"
    }

    @Test
    fun `string keyword missing key returns error`() {
        val yaml =
            yamlParse(
                """
                string2: foo-bar
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = StringKeyword("string1", TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Error<String>>().error shouldBe
            "key 'string1' not found at line 1 column 1"
    }

    @Test
    fun `string optional keyword`() {
        val yaml =
            yamlParse(
                """
                string2: foo-bar
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = StringKeyword("string1", TEST_KEYWORD_HELP).optional()
        keyword.parse(yaml.data).shouldBeTypeOf<Success<String?>>().data shouldBe null
    }

    @Test
    fun `string optional keyword returns value when key present`() {
        val yaml =
            yamlParse(
                """
                string1: foo-bar
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = StringKeyword("string1", TEST_KEYWORD_HELP).optional()
        keyword.parse(yaml.data).shouldBeTypeOf<Success<String?>>().data shouldBe "foo-bar"
    }

    @Test
    fun `string keyword with default`() {
        val yaml =
            yamlParse(
                """
                string2: foo-bar
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = StringKeyword("string1", TEST_KEYWORD_HELP).default("yolo2000")
        keyword.parse(yaml.data).shouldBeTypeOf<Success<String>>().data shouldBe "yolo2000"
    }

    @Test
    fun `string keyword with default returns value when key present`() {
        val yaml =
            yamlParse(
                """
                string1: foo-bar
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = StringKeyword("string1", TEST_KEYWORD_HELP).default("yolo2000")
        keyword.parse(yaml.data).shouldBeTypeOf<Success<String>>().data shouldBe "foo-bar"
    }

    @Test
    fun `string keyword max length`() {
        val yaml =
            yamlParse(
                """
                string1: hfjlkdshfjlkdahfjlkdshfjlkdshfjlkadhfjlkdajhflhfjlkdshfjlkdahfjlkdshfjlkdshfjlkadhfjlkdajhflhfjlkdshfjlkdahfjlkdshfjlkdshfjlkadhfjlkdajhfl
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = StringKeyword("string1", TEST_KEYWORD_HELP).constraints(StringConstraints(12, 0))
        keyword.parse(yaml.data).shouldBeTypeOf<Error<String>>().error shouldBe
            "maximum allowed length for 'string1' is 12 characters at line 1 column 1"
    }

    @Test
    fun `string keyword regex pattern fail`() {
        val yaml =
            yamlParse(
                """
                string1: ab_cd_12
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            StringKeyword("string1", TEST_KEYWORD_HELP)
                .constraints(StringConstraints(64, 0, "[a-z0-9]+(-[a-z0-9]+)*"))
        keyword.parse(yaml.data).shouldBeTypeOf<Error<String>>().error shouldBe
            "'string1' must match '[a-z0-9]+(-[a-z0-9]+)*' at line 1 column 1"
    }

    @Test
    fun `string keyword regex pattern success`() {
        val yaml =
            yamlParse(
                """
                string1: abc-def-123
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            StringKeyword("string1", TEST_KEYWORD_HELP)
                .constraints(StringConstraints(64, 0, "[a-z0-9]+(-[a-z0-9]+)*"))
        keyword.parse(yaml.data).shouldBeTypeOf<Success<String>>().data shouldBe "abc-def-123"
    }

    @Test
    fun `string keyword min length`() {
        val yaml =
            yamlParse(
                """
                string1: aa
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = StringKeyword("string1", TEST_KEYWORD_HELP).constraints(StringConstraints(12, 4))
        keyword.parse(yaml.data).shouldBeTypeOf<Error<String>>().error shouldBe
            "'string1' should be at least 4 characters long at line 1 column 1"
    }

    @Test
    fun `string keyword options failure`() {
        val yaml =
            yamlParse(
                """
                string1: xyz
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            StringKeyword("string1", TEST_KEYWORD_HELP)
                .constraints(StringConstraints(options = listOf("foo", "bar")))
        keyword.parse(yaml.data).shouldBeTypeOf<Error<String>>().error shouldBe
            "'xyz' is not allowed for 'string1', possible options are: 'foo', 'bar' at line 1 column 1"
    }

    @Test
    fun `string keyword options success`() {
        val yaml =
            yamlParse(
                """
                string1: foo
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            StringKeyword("string1", TEST_KEYWORD_HELP)
                .constraints(StringConstraints(options = listOf("foo", "bar")))
        keyword.parse(yaml.data).shouldBeTypeOf<Success<String>>().data shouldBe "foo"
    }

    @Test
    fun `string optional keyword applies constraints`() {
        val yaml =
            yamlParse(
                """
                string1: ab_cd_12
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = StringKeyword("string1", TEST_KEYWORD_HELP).constraints(StringConstraints.RFC_1123_NAME).optional()
        keyword.parse(yaml.data).shouldBeTypeOf<Error<String?>>().error shouldBe
            "'string1' must match '[a-z0-9]+(-[a-z0-9]+)*' at line 1 column 1"
    }

    @Test
    fun `string keyword with default applies constraints`() {
        val yaml =
            yamlParse(
                """
                string1: ab_cd_12
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = StringKeyword("string1", TEST_KEYWORD_HELP).constraints(StringConstraints.RFC_1123_NAME).default("foo-bar")
        keyword.parse(yaml.data).shouldBeTypeOf<Error<String>>().error shouldBe
            "'string1' must match '[a-z0-9]+(-[a-z0-9]+)*' at line 1 column 1"
    }

    @Test
    fun `string keyword exposes metadata`() {
        val required = StringKeyword("string1", TEST_KEYWORD_HELP)
        required.optional shouldBe false
        required.default shouldBe null

        val optional = StringKeyword("string1", TEST_KEYWORD_HELP).optional()
        optional.optional shouldBe true
        optional.default shouldBe null

        val withDefault = StringKeyword("string1", TEST_KEYWORD_HELP).default("yolo2000")
        withDefault.optional shouldBe true
        withDefault.default shouldBe "yolo2000"

        val withConstraints = StringKeyword("string1", TEST_KEYWORD_HELP).constraints(StringConstraints.RFC_1123_NAME)
        withConstraints.constraints shouldBe StringConstraints.RFC_1123_NAME
        withConstraints.optional shouldBe false
    }

    @Test
    fun `string keyword null value returns error`() {
        val yaml =
            yamlParse(
                """
                string1: null
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = StringKeyword("string1", TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Error<String>>().error shouldBe
            "key 'string1' is null at line 1 column 1"
    }

    @Test
    fun `string keyword empty value returns error`() {
        val yaml =
            yamlParse(
                """
                string1: ""
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = StringKeyword("string1", TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Error<String>>().error shouldBe
            "key 'string1' is empty at line 1 column 1"
    }

    @Test
    fun `string optional keyword null value returns null`() {
        val yaml =
            yamlParse(
                """
                string1: null
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = StringKeyword("string1", TEST_KEYWORD_HELP).optional()
        keyword.parse(yaml.data).shouldBeTypeOf<Success<String?>>().data shouldBe null
    }

    @Test
    fun `string optional keyword empty value returns null`() {
        val yaml =
            yamlParse(
                """
                string1: ""
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = StringKeyword("string1", TEST_KEYWORD_HELP).optional()
        keyword.parse(yaml.data).shouldBeTypeOf<Success<String?>>().data shouldBe null
    }

    @Test
    fun `string keyword with default null value returns default`() {
        val yaml =
            yamlParse(
                """
                string1: null
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = StringKeyword("string1", TEST_KEYWORD_HELP).default("yolo2000")
        keyword.parse(yaml.data).shouldBeTypeOf<Success<String>>().data shouldBe "yolo2000"
    }

    @Test
    fun `string keyword with default empty value returns default`() {
        val yaml =
            yamlParse(
                """
                string1: ""
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = StringKeyword("string1", TEST_KEYWORD_HELP).default("yolo2000")
        keyword.parse(yaml.data).shouldBeTypeOf<Success<String>>().data shouldBe "yolo2000"
    }

    @Test
    fun `string keyword non scalar value returns error`() {
        val yaml =
            yamlParse(
                """
                string1:
                  foo: bar
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = StringKeyword("string1", TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Error<String>>().error shouldBe
            "expected string but found '{'foo': 'bar'}'"
    }

    @Test
    fun `string keyword exact min length is allowed`() {
        val yaml =
            yamlParse(
                """
                string1: abcd
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = StringKeyword("string1", TEST_KEYWORD_HELP).constraints(StringConstraints(12, 4))
        keyword.parse(yaml.data).shouldBeTypeOf<Success<String>>().data shouldBe "abcd"
    }

    @Test
    fun `string keyword exact max length is allowed`() {
        val yaml =
            yamlParse(
                """
                string1: abcdefghijkl
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = StringKeyword("string1", TEST_KEYWORD_HELP).constraints(StringConstraints(12, 4))
        keyword.parse(yaml.data).shouldBeTypeOf<Success<String>>().data shouldBe "abcdefghijkl"
    }

    @Test
    fun `string keyword constraint violating default returns error when key missing`() {
        val yaml =
            yamlParse(
                """
                string2: foo
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            StringKeyword("string1", TEST_KEYWORD_HELP)
                .constraints(StringConstraints(options = listOf("foo", "bar")))
                .default("xyz")
        keyword.parse(yaml.data).shouldBeTypeOf<Error<String>>().error shouldBe
            "'xyz' is not allowed for 'string1', possible options are: 'foo', 'bar' at line 1 column 1"
    }
}
