package de.solidblocks.cloud.keywords

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.TEST_KEYWORD_HELP
import de.solidblocks.cloud.configuration.OptionalStringMapKeyword
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.yamlParse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class OptionalStringMapKeywordsTest {

    @Test
    fun `string map keyword`() {
        val yaml =
            yamlParse(
                """
                map1:
                    key1: value1
                    key2: value2
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = OptionalStringMapKeyword("map1", TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Map<String, String>?>>().data shouldBe
            mapOf("key1" to "value1", "key2" to "value2")
    }

    @Test
    fun `string map keyword missing key returns null`() {
        val yaml =
            yamlParse(
                """
                map2:
                    key1: value1
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = OptionalStringMapKeyword("map1", TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Map<String, String>?>>().data shouldBe null
    }

    @Test
    fun `string map keyword empty map`() {
        val yaml =
            yamlParse(
                """
                map1: {}
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = OptionalStringMapKeyword("map1", TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Success<Map<String, String>?>>().data shouldBe
            emptyMap<String, String>()
    }

    @Test
    fun `string map keyword non string value returns error`() {
        val yaml =
            yamlParse(
                """
                map1:
                    key1:
                        nested: value1
                """
                    .trimIndent(),
            )
                .shouldBeTypeOf<Success<YamlNode>>()

        val keyword = OptionalStringMapKeyword("map1", TEST_KEYWORD_HELP)
        keyword.parse(yaml.data).shouldBeTypeOf<Error<Map<String, String>?>>().error shouldBe
            "found non string value in map 'map1' at key(s) key1"
    }
}
