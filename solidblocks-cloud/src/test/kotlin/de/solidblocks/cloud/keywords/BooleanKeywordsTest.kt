package de.solidblocks.cloud.keywords

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.TEST_KEYWORD_HELP
import de.solidblocks.cloud.configuration.BooleanKeyword
import de.solidblocks.cloud.configuration.default
import de.solidblocks.cloud.configuration.optional
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.yamlParse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class BooleanKeywordsTest {

    @Test
    fun `boolean keyword`() {
        val yaml =
            yamlParse(
                """
                flag1: true
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = BooleanKeyword("flag1", TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Boolean>>().data shouldBe true
    }

    @Test
    fun `boolean keyword missing key returns error`() {
        val yaml =
            yamlParse(
                """
                flag2: true
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = BooleanKeyword("flag1", TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Error<Boolean>>().error shouldBe
            "key 'flag1' not found at line 1 column 1"
    }

    @Test
    fun `boolean keyword invalid value returns error`() {
        val yaml =
            yamlParse(
                """
                flag1: yolo
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = BooleanKeyword("flag1", TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Error<Boolean>>().error shouldBe
            "expected 'true' or 'false' but got 'yolo' at line 1 column 1"
    }

    @Test
    fun `boolean keyword optional`() {
        val yaml =
            yamlParse(
                """
                flag2: true
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = BooleanKeyword("flag1", TEST_KEYWORD_HELP).optional()
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Boolean?>>().data shouldBe null
    }

    @Test
    fun `boolean keyword optional returns value when key present`() {
        val yaml =
            yamlParse(
                """
                flag1: false
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = BooleanKeyword("flag1", TEST_KEYWORD_HELP).optional()
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Boolean?>>().data shouldBe false
    }

    @Test
    fun `boolean keyword with default`() {
        val yaml =
            yamlParse(
                """
                flag2: false
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = BooleanKeyword("flag1", TEST_KEYWORD_HELP).default(true)
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Boolean>>().data shouldBe true
    }

    @Test
    fun `boolean keyword with default returns value when key present`() {
        val yaml =
            yamlParse(
                """
                flag1: false
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = BooleanKeyword("flag1", TEST_KEYWORD_HELP).default(true)
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Boolean>>().data shouldBe false
    }

    @Test
    fun `boolean keyword with default returns error for invalid value`() {
        val yaml =
            yamlParse(
                """
                flag1: yolo
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = BooleanKeyword("flag1", TEST_KEYWORD_HELP).default(true)
        keyword.parse(yaml.data).shouldBeTypeOf<Error<Boolean>>().error shouldBe
            "expected 'true' or 'false' but got 'yolo' at line 1 column 1"
    }

    @Test
    fun `boolean keyword exposes metadata`() {
        val required = BooleanKeyword("flag1", TEST_KEYWORD_HELP)
        required.optional shouldBe false
        required.default shouldBe null

        val optional = BooleanKeyword("flag1", TEST_KEYWORD_HELP).optional()
        optional.optional shouldBe true
        optional.default shouldBe null

        val withDefault = BooleanKeyword("flag1", TEST_KEYWORD_HELP).default(true)
        withDefault.optional shouldBe true
        withDefault.default shouldBe true
    }

    @Test
    fun `boolean keyword null value returns error`() {
        val yaml =
            yamlParse(
                """
                flag1: null
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = BooleanKeyword("flag1", TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Error<Boolean>>().error shouldBe
            "key 'flag1' is null at line 1 column 1"
    }

    @Test
    fun `boolean keyword empty value returns error`() {
        val yaml =
            yamlParse(
                """
                flag1: ""
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = BooleanKeyword("flag1", TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Error<Boolean>>().error shouldBe
            "key 'flag1' is empty at line 1 column 1"
    }

    @Test
    fun `boolean keyword optional null value returns null`() {
        val yaml =
            yamlParse(
                """
                flag1: null
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = BooleanKeyword("flag1", TEST_KEYWORD_HELP).optional()
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Boolean?>>().data shouldBe null
    }

    @Test
    fun `boolean keyword with default null value returns default`() {
        val yaml =
            yamlParse(
                """
                flag1: null
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = BooleanKeyword("flag1", TEST_KEYWORD_HELP).default(true)
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Boolean>>().data shouldBe true
    }

    @Test
    fun `boolean keyword with default empty value returns default`() {
        val yaml =
            yamlParse(
                """
                flag1: ""
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = BooleanKeyword("flag1", TEST_KEYWORD_HELP).default(true)
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Boolean>>().data shouldBe true
    }

    @Test
    fun `boolean keyword is case sensitive`() {
        val yaml =
            yamlParse(
                """
                flag1: "True"
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = BooleanKeyword("flag1", TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Error<Boolean>>().error shouldBe
            "expected 'true' or 'false' but got 'True' at line 1 column 1"
    }

    @Test
    fun `boolean keyword rejects yaml 1-1 style literals`() {
        val yaml =
            yamlParse(
                """
                flag1: yes
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = BooleanKeyword("flag1", TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Error<Boolean>>().error shouldBe
            "expected 'true' or 'false' but got 'yes' at line 1 column 1"
    }
}
