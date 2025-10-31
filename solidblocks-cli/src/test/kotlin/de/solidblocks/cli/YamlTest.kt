package de.solidblocks.cli

import com.charleskorn.kaml.yamlMap
import de.solidblocks.cli.utils.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

val HELP = KeywordHelp("some-example", "some-description")

class YamlTest {

  @Test
  fun testParse() {
    val rawYaml =
        """
            root-key1:
              - command: ping
              - file: /some/file
        """
            .trimIndent()

    val result = yamlParse(rawYaml)
    assertTrue(result is Success)
  }

  @Test
  fun testValueForKeywordShorthand() {
    val rawYaml =
        """
            command: ping
        """
            .trimIndent()

    val result = yamlParse(rawYaml) as Success
    val value = result.data.valueForKeyword(Keyword("command", KeywordType.STRING, HELP)) as Success
    assertEquals("ping", value.data)
  }

  @Test
  fun testValueForKeywordExtended() {
    val rawYaml =
        """
            command:
            name: ping
        """
            .trimIndent()

    val result = yamlParse(rawYaml) as Success

    val value = result.data.valueForKeyword(Keyword("command", KeywordType.STRING, HELP))
    assertTrue(value is Empty)
  }

  @Test
  fun testValueForKeywordError() {
    val result = yamlParse("---") as Success

    val value = result.data.valueForKeyword(Keyword("command", KeywordType.STRING, HELP)) as Error
    assertEquals("expected a map, got 'null'", value.error)
  }

  @Test
  fun testValueForKeywordNotFound() {
    val rawYaml =
        """
            command: ping
        """
            .trimIndent()

    val result = yamlParse(rawYaml) as Success

    val value = result.data.valueForKeyword(Keyword("command1", KeywordType.STRING, HELP))
    assertTrue(value is Empty)
  }

  @Test
  fun testValueForKeywordWithFallback() {
    val rawYaml =
        """
            command: ping
        """
            .trimIndent()

    val result = yamlParse(rawYaml) as Success

    val value =
        result.data.valueForKeyword(
            Keyword("command", KeywordType.STRING, HELP),
            Keyword("command1", KeywordType.STRING, HELP),
        ) as Success
    assertEquals("ping", value.data)
  }

  @Test
  fun testParseError() {
    val rawYaml =
        """
            %§${'$'}"%"§${'$'}%§"
        """
            .trimIndent()

    val result = yamlParse(rawYaml) as Error
    assertEquals("invalid yml", result.error)
  }

  @Test
  fun testGetList() {
    val rawYaml =
        """
            key1:
              - item1: string1
              - item2: string2
        """
            .trimIndent()

    val result = yamlParse(rawYaml) as Success

    val list = result.data.yamlMap.getList("key1") as Success
    assertEquals(2, list.data.items.size)
  }

  @Test
  fun testGetListError() {
    val rawYaml =
        """
            key1:
              item1: string1
              item2: string2
        """
            .trimIndent()

    val result = yamlParse(rawYaml) as Success
    val list = result.data.yamlMap.getList("key1") as Error
    assertEquals("'key1' should be a list line 1 colum 1", list.error)
  }

  @Test
  fun testGetKeys() {
    val rawYaml =
        """
              item1: string1
              item2: string2
        """
            .trimIndent()

    val result = yamlParse(rawYaml) as Success
    val keys = result.data.getKeys() as Success
    assertEquals(2, keys.data.size)
    assertEquals("item1", keys.data[0])
    assertEquals("item2", keys.data[1])
  }
}
