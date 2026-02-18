package de.solidblocks.cli

import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.Keyword
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.utils.*
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class YamlTest {

  @Test
  fun testParse() {
    val rawYaml =
        """
        key1:
          foo: bar
        """
            .trimIndent()

    yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
  }

  @Test
  fun testParseError() {
    val rawYaml =
        """
        %§${'$'}"%"§${'$'}%§"
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Error<YamlNode>>()
    result.error shouldBe "invalid yaml document"
  }

  @Test
  fun testParseEmpty() {
    val result = yamlParse("").shouldBeTypeOf<YamlEmpty<YamlNode>>()
    result.message shouldBe "yaml document is empty"
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

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val list = result.data.getList("key1").shouldBeTypeOf<Success<YamlList>>()
    list.data.items shouldHaveSize 2
  }

  @Test
  fun testGetListEmpty() {
    val rawYaml =
        """
        key2:
          - item1: string1
          - item2: string2
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val list = result.data.getList("key1").shouldBeTypeOf<YamlEmpty<YamlNode>>()
    list.message shouldBe "no list found for key 'key1' at line 1 colum 1"
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

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val list = result.data.getList("key1").shouldBeTypeOf<Error<YamlNode>>()
    list.error shouldBe "key 'key1' should be a list line 1 colum 1"
  }

  @Test
  fun testGetString() {
    val rawYaml =
        """
        key1: "foo"
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val string = result.data.getString("key1").shouldBeTypeOf<Success<String?>>()
    assertEquals("foo", string.data)
  }

  @Test
  fun testGetStringEmpty() {
    val rawYaml =
        """
        key1: ""
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val string = result.data.getString("key1").shouldBeTypeOf<YamlEmpty<String?>>()
    string.message shouldBe "key 'key1' is empty at line 1 colum 1"

    val nonNullOrEmptyString =
        result.data.getNonNullOrEmptyString("key1").shouldBeTypeOf<Error<String>>()
    nonNullOrEmptyString.error shouldBe "key 'key1' is empty at line 1 colum 1"
  }

  @Test
  fun testGetStringNull() {
    val rawYaml =
        """
        key1: null
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val string = result.data.getString("key1").shouldBeTypeOf<YamlEmpty<String?>>()
    string.message shouldBe "key 'key1' is null at line 1 colum 1"

    val nonNullOrEmptyString =
        result.data.getNonNullOrEmptyString("key1").shouldBeTypeOf<Error<String>>()
    nonNullOrEmptyString.error shouldBe "key 'key1' is null at line 1 colum 1"
  }

  @Test
  fun testGetEmptyString() {
    val rawYaml =
        """
        key1: 
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val string = result.data.getString("key1").shouldBeTypeOf<YamlEmpty<String>>()
    string.message shouldBe "key 'key1' is null at line 1 colum 1"

    val nonNullOrEmptyString =
        result.data.getNonNullOrEmptyString("key1").shouldBeTypeOf<Error<String>>()
    nonNullOrEmptyString.error shouldBe "key 'key1' is null at line 1 colum 1"
  }

  @Test
  fun testGetStringNoKey() {
    val rawYaml =
        """
        key2: "bar"
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val string = result.data.getString("key1").shouldBeTypeOf<YamlEmpty<String?>>()
    assertEquals("key 'key1' not found at line 1 colum 1", string.message)
  }

  @Test
  fun testGetBoolean() {
    val rawYaml =
        """
        key1: false
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val bool = result.data.getBoolean("key1").shouldBeTypeOf<Success<Boolean>>()
    bool.data shouldBe false

    val optionalBool = result.data.getOptionalBoolean("key1").shouldBeTypeOf<Success<Boolean?>>()
    optionalBool.data shouldBe false
  }

  @Test
  fun testGetBooleanEmpty() {
    val rawYaml =
        """
        key1: ""
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val bool = result.data.getBoolean("key1").shouldBeTypeOf<Error<Boolean>>()
    bool.error shouldBe "key 'key1' is empty at line 1 colum 1"

    val optionalBool = result.data.getOptionalBoolean("key1").shouldBeTypeOf<Success<Boolean?>>()
    optionalBool.data shouldBe null
  }

  @Test
  fun testGetBooleanInvalid() {
    val rawYaml =
        """
        key1: "yolo"
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val string = result.data.getBoolean("key1").shouldBeTypeOf<Error<Boolean?>>()
    assertEquals("expected 'true' or 'false' but got 'yolo' at line 1 colum 1", string.error)
  }

  @Test
  fun testGetBooleanNull() {
    val rawYaml =
        """
        key1: null
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val string = result.data.getBoolean("key1").shouldBeTypeOf<Error<Boolean?>>()
    string.error shouldBe "key 'key1' is null at line 1 colum 1"
  }

  @Test
  fun testGetBooleanNoValue() {
    val rawYaml =
        """
        key1: 
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val string = result.data.getBoolean("key1").shouldBeTypeOf<Error<String?>>()
    string.error shouldBe "key 'key1' is null at line 1 colum 1"
  }

  @Test
  fun testGetBooleanNoKey() {
    val rawYaml =
        """
        key2: "bar"
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val string = result.data.getBoolean("key1").shouldBeTypeOf<Error<Boolean?>>()
    string.error shouldBe "key 'key1' not found at line 1 colum 1"
  }

  @Test
  fun testGetMap() {
    val rawYaml =
        """
        key1:
          item1: string1
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val map = result.data.getMap("key1").shouldBeTypeOf<Success<YamlMap>>()
    map.data.entries.count() shouldBe 1
  }

  @Test
  fun testGetMapListError() {
    val rawYaml =
        """
        key1:
          - item1: string1
          - item2: string2
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val map = result.data.getMap("key1").shouldBeTypeOf<Error<YamlMap>>()
    map.error shouldBe "key 'key1' should be a map line 1 colum 1"
  }

  @Test
  fun testGetMapEmpty() {
    val rawYaml =
        """
        key1:
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val map = result.data.getMap("key1").shouldBeTypeOf<Error<YamlMap>>()
    map.error shouldBe "key 'key1' should be a map line 1 colum 1"
  }

  @Test
  fun testGetMapEmpty1() {
    val rawYaml =
        """
        key1: {}
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    result.data.getMap("key1").shouldBeTypeOf<Success<YamlMap>>()
  }

  @Test
  fun testGetMapEmpty2() {
    val rawYaml =
        """
        key2: {}
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val map = result.data.getMap("key1").shouldBeTypeOf<YamlEmpty<YamlMap>>()
    map.message shouldBe "no map found for key 'key1' at line 1 colum 1"
  }

  @Test
  fun testGetNumberNull() {
    val rawYaml =
        """
        key1: null
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val number = result.data.getNumber("key1").shouldBeTypeOf<Error<Number?>>()
    number.error shouldBe "key 'key1' is null at line 1 colum 1"
  }

  @Test
  fun testGetNumberNoValue() {
    val rawYaml =
        """
        key1: 
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val string = result.data.getBoolean("key1").shouldBeTypeOf<Error<Boolean?>>()
    string.error shouldBe "key 'key1' is null at line 1 colum 1"
  }

  @Test
  fun testGetNumberEmptyString() {
    val rawYaml =
        """
        key1: "" 
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val number = result.data.getNumber("key1").shouldBeTypeOf<Error<Number?>>()
    number.error shouldBe "key 'key1' is empty at line 1 colum 1"
  }

  @Test
  fun testGetNumber() {
    val rawYaml =
        """
        key1: 123
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val number = result.data.getNumber("key1").shouldBeTypeOf<Success<Number?>>()
    number.data shouldBe 123
  }

  @Test
  fun testGetNumberNoKey() {
    val rawYaml =
        """
        key2: "bar"
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val number = result.data.getNumber("key1").shouldBeTypeOf<Error<Number?>>()
    assertEquals("key 'key1' not found at line 1 colum 1", number.error)
  }

  interface BaseType

  class Type1 : BaseType

  class Type2 : BaseType

  class Type1Factory : PolymorphicConfigurationFactory<Type1>() {

    override val help: ConfigurationHelp
      get() = TODO("Not yet implemented")

    override val keywords: List<Keyword<*>>
      get() = TODO("Not yet implemented")

    override fun parse(yaml: YamlNode): Result<Type1> = Success(Type1())
  }

  class Type2Factory : PolymorphicConfigurationFactory<Type2>() {
    override val help: ConfigurationHelp
      get() = TODO("Not yet implemented")

    override val keywords: List<Keyword<*>>
      get() = TODO("Not yet implemented")

    override fun parse(yaml: YamlNode): Result<Type2> = Success(Type2())
  }

  @Test
  fun testGetPolymorphicList() {
    val rawYaml =
        """
        list1:
            - type: type1
            - type: type2
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val list =
        result.data.getPolymorphicList(
            "list1",
            mapOf("type1" to Type1Factory(), "type2" to Type2Factory()),
        )

    list.shouldBeTypeOf<Success<List<BaseType>>>()
    list.data shouldHaveSize 2
  }

  @Test
  fun testGetPolymorphicListTypeMissing() {
    val rawYaml =
        """
        list1:
            - name: name1
            - type: type2
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val list =
        result.data
            .getPolymorphicList(
                "list1",
                mapOf("type1" to Type1Factory(), "type2" to Type2Factory()),
            )
            .shouldBeTypeOf<Error<YamlNode>>()

    list.error shouldBe "key 'type' not found at line 2 colum 7"
  }

  @Test
  fun testGetPolymorphicListInvalidType() {
    val rawYaml =
        """
        list1:
            - type: type123
            - type: type2
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val list =
        result.data
            .getPolymorphicList(
                "list1",
                mapOf("type1" to Type1Factory(), "type2" to Type2Factory()),
            )
            .shouldBeTypeOf<Error<YamlNode>>()

    list.error shouldBe
        "unknown type 'type123', possible types are 'type1', 'type2' at line 2 colum 7"
  }

  @Test
  fun testGetPolymorphicListTypeEmpty() {
    val rawYaml =
        """
        list1:
            - type:
            - type: type2
        """
            .trimIndent()

    val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    val list =
        result.data
            .getPolymorphicList(
                "list1",
                mapOf("type1" to Type1Factory(), "type2" to Type2Factory()),
            )
            .shouldBeTypeOf<Error<YamlNode>>()

    list.error shouldBe "key 'type' is null at line 2 colum 7"
  }
}
