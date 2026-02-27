package de.solidblocks.cloud

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.StringKeywordOptional
import de.solidblocks.cloud.configuration.StringConstraints
import de.solidblocks.cloud.configuration.StringConstraints.Companion.NONE
import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.configuration.StringKeywordOptionalWithDefault
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.yamlParse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class StringKeywordsTest {

    @Test
    fun testStringKeyword() {
        val yaml =
            yamlParse(
                """
                string1: foo-bar
                """
                    .trimIndent(),
            ).shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            StringKeyword(
                "string1",
                NONE,
                TEST_KEYWORD_HELP
            )
        keyword.parse(yaml.data).shouldBeTypeOf<Success<String>>().data shouldBe "foo-bar"
    }

    @Test
    fun testStringOptionalKeyword() {
        val yaml =
            yamlParse(
                """
                string2: foo-bar
                """
                    .trimIndent(),
            ).shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            StringKeywordOptional(
                "string1",
                NONE,
                TEST_KEYWORD_HELP
            )
        keyword.parse(yaml.data).shouldBeTypeOf<Success<String>>().data shouldBe null
    }

    @Test
    fun testStringOptionalKeywordWithDefault() {
        val yaml =
            yamlParse(
                """
                string2: foo-bar
                """
                    .trimIndent(),
            ).shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            StringKeywordOptionalWithDefault(
                "string1",
                NONE,
                "yolo2000",
                TEST_KEYWORD_HELP
            )
        keyword.parse(yaml.data).shouldBeTypeOf<Success<String>>().data shouldBe "yolo2000"
    }

    @Test
    fun testStringKeywordMaxLength() {
        val yaml =
            yamlParse(
                """
                string1: hfjlkdshfjlkdahfjlkdshfjlkdshfjlkadhfjlkdajhflhfjlkdshfjlkdahfjlkdshfjlkdshfjlkadhfjlkdajhflhfjlkdshfjlkdahfjlkdshfjlkdshfjlkadhfjlkdajhfl
                """
                    .trimIndent(),
            ).shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            StringKeyword(
                "string1",
                StringConstraints(12, 0),
                TEST_KEYWORD_HELP
            )
        keyword.parse(yaml.data).shouldBeTypeOf<Error<String>>().error shouldBe "maximum allowed length for 'string1' is 12 characters at line 1 colum 1"
    }

    @Test
    fun testStringKeywordRegexPatternFail() {
        val yaml =
            yamlParse(
                """
                string1: ab_cd_12
                """
                    .trimIndent(),
            ).shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            StringKeyword(
                "string1",
                StringConstraints(64, 0, "[a-z0-9]+(-[a-z0-9]+)*"),
                TEST_KEYWORD_HELP
            )
        keyword.parse(yaml.data).shouldBeTypeOf<Error<String>>().error shouldBe "'string1' must match '[a-z0-9]+(-[a-z0-9]+)*' at line 1 colum 1"
    }

    @Test
    fun testStringKeywordRegexPatternSuccess() {
        val yaml =
            yamlParse(
                """
                string1: abc-def-123
                """
                    .trimIndent(),
            ).shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            StringKeyword(
                "string1",
                StringConstraints(64, 0, "[a-z0-9]+(-[a-z0-9]+)*"),
                TEST_KEYWORD_HELP
            )
        keyword.parse(yaml.data).shouldBeTypeOf<Success<String>>().data shouldBe "abc-def-123"
    }

    @Test
    fun testStringKeywordMinLength() {
        val yaml =
            yamlParse(
                """
                string1: aa
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            StringKeyword(
                "string1",
                StringConstraints(12, 4),
                TEST_KEYWORD_HELP
            )
        keyword.parse(yaml.data).shouldBeTypeOf<Error<String>>().error shouldBe "'string1' should be at least 4 characters long at line 1 colum 1"
    }

    @Test
    fun testOptionalStringKeyword() {

        val yaml =
            yamlParse(
                """
                string1: foo-bar
                """
                    .trimIndent(),
            ).shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            StringKeywordOptional(
                "string2",
                NONE,
                TEST_KEYWORD_HELP
            )
        keyword.parse(yaml.data).shouldBeTypeOf<Success<String?>>().data shouldBe null
    }

    @Test
    fun testStringKeywordOptionsFailure() {
        val yaml =
            yamlParse(
                """
                string1: xyz
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            StringKeyword(
                "string1",
                StringConstraints(options = listOf("foo", "bar")),
                TEST_KEYWORD_HELP
            )
        keyword.parse(yaml.data).shouldBeTypeOf<Error<String>>().error shouldBe "'xyz' is not allowed for 'string1', possible options are: 'foo', 'bar' at line 1 colum 1"
    }

    @Test
    fun testStringKeywordOptionsSuccess() {
        val yaml =
            yamlParse(
                """
                string1: foo
                """
                    .trimIndent(),
            ).shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            StringKeyword(
                "string1",
                StringConstraints(options = listOf("foo", "bar")),
                TEST_KEYWORD_HELP
            )
        keyword.parse(yaml.data).shouldBeTypeOf<Success<String>>().data shouldBe "foo"
    }

}