package de.solidblocks.cloud

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.NumberConstraints
import de.solidblocks.cloud.configuration.NumberKeyword
import de.solidblocks.cloud.configuration.NumberKeywordOptional
import de.solidblocks.cloud.configuration.NumberKeywordOptionalWithDefault
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.yamlParse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class NumberKeywordsTest {

    @Test
    fun testNumberKeyword() {
        val yaml =
            yamlParse(
                """
                number1: 123
                """
                    .trimIndent(),
            ).shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            NumberKeyword(
                "number1",
                NumberConstraints.NONE,
                TEST_KEYWORD_HELP
            )
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Int>>().data shouldBe 123
    }

    @Test
    fun testNumberKeywordOptional() {
        val yaml =
            yamlParse(
                """
                number2: 123
                """
                    .trimIndent(),
            ).shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            NumberKeywordOptional(
                "number1",
                NumberConstraints.NONE,
                TEST_KEYWORD_HELP,
            )
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Int>>().data shouldBe null
    }

    @Test
    fun testNumberKeywordOptionalWithDefault() {
        val yaml =
            yamlParse(
                """
                number2: 123
                """
                    .trimIndent(),
            ).shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            NumberKeywordOptionalWithDefault(
                "number1",
                NumberConstraints.NONE,
                TEST_KEYWORD_HELP,
                124
            )
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Int>>().data shouldBe 124
    }

    @Test
    fun testNumberKeywordMin() {
        val yaml =
            yamlParse(
                """
                number1: 21
                """
                    .trimIndent(),
            ).shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            NumberKeyword(
                "number1",
                NumberConstraints(min = 22),
                TEST_KEYWORD_HELP,
            )
        keyword.parse(yaml.data).shouldBeTypeOf<Error<Int>>().error shouldBe "'number1' may not be smaller then 22 at line 1 colum 1"
    }

    @Test
    fun testNumberKeywordMax() {
        val yaml =
            yamlParse(
                """
                number1: 21
                """
                    .trimIndent(),
            ).shouldBeTypeOf<Success<YamlNode>>()

        val keyword =
            NumberKeyword(
                "number1",
                NumberConstraints(max = 12),
                TEST_KEYWORD_HELP,
            )
        keyword.parse(yaml.data).shouldBeTypeOf<Error<Int>>().error shouldBe "'number1' may not be larger then null at line 1 colum 1"
    }


}