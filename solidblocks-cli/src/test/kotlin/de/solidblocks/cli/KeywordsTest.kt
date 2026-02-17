package de.solidblocks.cli

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.KeywordHelp
import de.solidblocks.cloud.configuration.OptionalStringKeyword
import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.yamlParse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class KeywordsTest {

  @Test
  fun testStringKeyword() {
    val yaml =
        yamlParse(
                """
                string1: foo-bar
                """
                    .trimIndent(),
            )
            .shouldBeTypeOf<Success<YamlNode>>()

    val stringKeyword =
        StringKeyword(
            "string1",
            KeywordHelp(
                "",
                "path to the private key, if not set, the default SSH key paths will be tried.",
            ),
        )
    stringKeyword.parse(yaml.data).shouldBeTypeOf<Success<String>>().data shouldBe "foo-bar"
  }

  @Test
  fun testOptionalStringKeyword() {
    val yaml =
        yamlParse(
                """
                string1: foo-bar
                """
                    .trimIndent(),
            )
            .shouldBeTypeOf<Success<YamlNode>>()

    val optionalStringKeyword =
        OptionalStringKeyword(
            "string2",
            KeywordHelp(
                "",
                "",
            ),
        )
    optionalStringKeyword.parse(yaml.data).shouldBeTypeOf<Success<String?>>().data shouldBe null
  }
}
