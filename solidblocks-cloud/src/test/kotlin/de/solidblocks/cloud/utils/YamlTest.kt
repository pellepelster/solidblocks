package de.solidblocks.cloud.utils

import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.yamlMap
import de.solidblocks.cloud.configuration.ConfigurationFactory
import de.solidblocks.cloud.configuration.Keyword
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class YamlTest {
    @Test
    fun `parse`() {
        val rawYaml =
            """
        key1:
          foo: bar
        """
                .trimIndent()

        yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
    }

    @Test
    fun `parse error`() {
        val rawYaml =
            """
        %§${'$'}"%"§${'$'}%§"
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Error<YamlNode>>()
        result.error shouldBe "invalid yaml document"
    }

    @Test
    fun `parse empty`() {
        val result = yamlParse("").shouldBeTypeOf<YamlEmpty<YamlNode>>()
        result.message shouldBe "yaml document is empty"
    }

    @Test
    fun `get list`() {
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
    fun `get list empty`() {
        val rawYaml =
            """
        key2:
          - item1: string1
          - item2: string2
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val list = result.data.getList("key1").shouldBeTypeOf<YamlEmpty<YamlNode>>()
        list.message shouldBe "no list found for key 'key1' at line 1 column 1"
    }

    @Test
    fun `get list error`() {
        val rawYaml =
            """
        key1:
          item1: string1
          item2: string2
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val list = result.data.getList("key1").shouldBeTypeOf<Error<YamlNode>>()
        list.error shouldBe "key 'key1' should be a list line 1 column 1"
    }

    @Test
    fun `get string`() {
        val rawYaml =
            """
        key1: "foo"
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val string = result.data.getString("key1").shouldBeTypeOf<Success<String?>>()
        Assertions.assertEquals("foo", string.data)
    }

    @Test
    fun `get string empty`() {
        val rawYaml =
            """
        key1: ""
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val string = result.data.getString("key1").shouldBeTypeOf<YamlEmpty<String?>>()
        string.message shouldBe "key 'key1' is empty at line 1 column 1"

        val nonNullOrEmptyString =
            result.data.getNonNullOrEmptyString("key1").shouldBeTypeOf<Error<String>>()
        nonNullOrEmptyString.error shouldBe "key 'key1' is empty at line 1 column 1"
    }

    @Test
    fun `get string null`() {
        val rawYaml =
            """
        key1: null
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val string = result.data.getString("key1").shouldBeTypeOf<YamlEmpty<String?>>()
        string.message shouldBe "key 'key1' is null at line 1 column 1"

        val nonNullOrEmptyString =
            result.data.getNonNullOrEmptyString("key1").shouldBeTypeOf<Error<String>>()
        nonNullOrEmptyString.error shouldBe "key 'key1' is null at line 1 column 1"
    }

    @Test
    fun `get empty string`() {
        val rawYaml =
            """
        key1: 
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val string = result.data.getString("key1").shouldBeTypeOf<YamlEmpty<String>>()
        string.message shouldBe "key 'key1' is null at line 1 column 1"

        val nonNullOrEmptyString =
            result.data.getNonNullOrEmptyString("key1").shouldBeTypeOf<Error<String>>()
        nonNullOrEmptyString.error shouldBe "key 'key1' is null at line 1 column 1"
    }

    @Test
    fun `get string no key`() {
        val rawYaml =
            """
        key2: "bar"
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val string = result.data.getString("key1").shouldBeTypeOf<YamlEmpty<String?>>()
        Assertions.assertEquals("key 'key1' not found at line 1 column 1", string.message)
    }

    @Test
    fun `get boolean`() {
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
    fun `get boolean empty`() {
        val rawYaml =
            """
        key1: ""
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val bool = result.data.getBoolean("key1").shouldBeTypeOf<Error<Boolean>>()
        bool.error shouldBe "key 'key1' is empty at line 1 column 1"

        val optionalBool = result.data.getOptionalBoolean("key1").shouldBeTypeOf<Success<Boolean?>>()
        optionalBool.data shouldBe null
    }

    @Test
    fun `get boolean invalid`() {
        val rawYaml =
            """
        key1: "yolo"
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val string = result.data.getBoolean("key1").shouldBeTypeOf<Error<Boolean?>>()
        Assertions.assertEquals("expected 'true' or 'false' but got 'yolo' at line 1 column 1", string.error)
    }

    @Test
    fun `get boolean null`() {
        val rawYaml =
            """
        key1: null
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val string = result.data.getBoolean("key1").shouldBeTypeOf<Error<Boolean?>>()
        string.error shouldBe "key 'key1' is null at line 1 column 1"
    }

    @Test
    fun `get boolean no value`() {
        val rawYaml =
            """
        key1: 
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val string = result.data.getBoolean("key1").shouldBeTypeOf<Error<String?>>()
        string.error shouldBe "key 'key1' is null at line 1 column 1"
    }

    @Test
    fun `get boolean no key`() {
        val rawYaml =
            """
        key2: "bar"
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val string = result.data.getBoolean("key1").shouldBeTypeOf<Error<Boolean?>>()
        string.error shouldBe "key 'key1' not found at line 1 column 1"
    }

    @Test
    fun `get map`() {
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
    fun `get string map`() {
        val rawYaml =
            """
        key1:
          item1: string1
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val map = result.data.getStringMap("key1").shouldBeTypeOf<Success<Map<String, String>>>()
        map.data.entries.count() shouldBe 1
        map.data["item1"] shouldBe "string1"
    }

    @Test
    fun `get string map non string`() {
        val rawYaml =
            """
        key1:
          item1:
            - foo: bar
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val map = result.data.getStringMap("key1").shouldBeTypeOf<Error<Map<String, String>>>()
        map.error shouldBe "found non string value in map 'key1' at key(s) item1"
    }

    @Test
    fun `get map list error`() {
        val rawYaml =
            """
        key1:
          - item1: string1
          - item2: string2
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val map = result.data.getMap("key1").shouldBeTypeOf<Error<YamlMap>>()
        map.error shouldBe "key 'key1' should be a map line 1 column 1"
    }

    @Test
    fun `get map empty`() {
        val rawYaml =
            """
        key1:
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val map = result.data.getMap("key1").shouldBeTypeOf<Error<YamlMap>>()
        map.error shouldBe "key 'key1' should be a map line 1 column 1"
    }

    @Test
    fun `get map empty1`() {
        val rawYaml =
            """
        key1: {}
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        result.data.getMap("key1").shouldBeTypeOf<Success<YamlMap>>()
    }

    @Test
    fun `get map empty2`() {
        val rawYaml =
            """
        key2: {}
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val map = result.data.getMap("key1").shouldBeTypeOf<YamlEmpty<YamlMap>>()
        map.message shouldBe "no map found for key 'key1' at line 1 column 1"
    }

    @Test
    fun `get number null`() {
        val rawYaml =
            """
        key1: null
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val number = result.data.getNumber("key1").shouldBeTypeOf<Error<Number?>>()
        number.error shouldBe "key 'key1' is null at line 1 column 1"
    }

    @Test
    fun `get number no value`() {
        val rawYaml =
            """
        key1: 
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val string = result.data.getBoolean("key1").shouldBeTypeOf<Error<Boolean?>>()
        string.error shouldBe "key 'key1' is null at line 1 column 1"
    }

    @Test
    fun `get number empty string`() {
        val rawYaml =
            """
        key1: "" 
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val number = result.data.getNumber("key1").shouldBeTypeOf<Error<Number?>>()
        number.error shouldBe "key 'key1' is empty at line 1 column 1"
    }

    @Test
    fun `get number`() {
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
    fun `get number no key`() {
        val rawYaml =
            """
        key2: "bar"
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val number = result.data.getNumber("key1").shouldBeTypeOf<Error<Number?>>()
        Assertions.assertEquals("key 'key1' not found at line 1 column 1", number.error)
    }

    data class Item(val name: String)

    class ItemFactory : ConfigurationFactory<Item> {
        override val help: ConfigurationHelp
            get() = TODO("Not yet implemented")

        override val keywords: List<Keyword<*>>
            get() = TODO("Not yet implemented")

        override fun parse(yaml: YamlNode): Result<Item> = when (val name = yaml.getNonNullOrEmptyString("name")) {
            is Error<String> -> Error(name.error)
            is Success<String> -> Success(Item(name.data))
        }
    }

    class EntryCountFactory : ConfigurationFactory<Int> {
        override val help: ConfigurationHelp
            get() = TODO("Not yet implemented")

        override val keywords: List<Keyword<*>>
            get() = TODO("Not yet implemented")

        override fun parse(yaml: YamlNode): Result<Int> = Success(yaml.yamlMap.entries.count())
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
    fun `get polymorphic list`() {
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
    fun `get polymorphic list type missing`() {
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

        list.error shouldBe "key 'type' not found at line 2 column 7"
    }

    @Test
    fun `get polymorphic list invalid type`() {
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
            "unknown type 'type123', possible types are 'type1', 'type2' at line 2 column 7"
    }

    @Test
    fun `get polymorphic list type empty`() {
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

        list.error shouldBe "key 'type' is null at line 2 column 7"
    }

    @Test
    fun `parse whitespace only document`() {
        val result = yamlParse("   \n   ").shouldBeTypeOf<YamlEmpty<YamlNode>>()
        result.message shouldBe "yaml document is empty"
    }

    @Test
    fun `get number invalid`() {
        val rawYaml =
            """
        key1: "abc"
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val number = result.data.getNumber("key1").shouldBeTypeOf<Error<Number?>>()
        number.error shouldBe "expected number but got 'abc' at line 1 column 1"
    }

    @Test
    fun `get number decimal`() {
        val rawYaml =
            """
        key1: 1.5
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val number = result.data.getNumber("key1").shouldBeTypeOf<Error<Number?>>()
        number.error shouldBe "expected number but got '1.5' at line 1 column 1"
    }

    @Test
    fun `get number negative`() {
        val rawYaml =
            """
        key1: -42
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val number = result.data.getNumber("key1").shouldBeTypeOf<Success<Number?>>()
        number.data shouldBe -42
    }

    @Test
    fun `get number with default returns value when key present`() {
        val rawYaml =
            """
        key1: 7
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val number = result.data.getNumber("key1", 5).shouldBeTypeOf<Success<Number>>()
        number.data shouldBe 7
    }

    @Test
    fun `get number with default returns default when key missing`() {
        val rawYaml =
            """
        key2: 7
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val number = result.data.getNumber("key1", 5).shouldBeTypeOf<Success<Number>>()
        number.data shouldBe 5
    }

    @Test
    fun `get number with default returns default when value is null`() {
        val rawYaml =
            """
        key1: null
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val number = result.data.getNumber("key1", 5).shouldBeTypeOf<Success<Number>>()
        number.data shouldBe 5
    }

    @Test
    fun `get number with default returns error for invalid value`() {
        val rawYaml =
            """
        key1: "abc"
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val number = result.data.getNumber("key1", 5).shouldBeTypeOf<Error<Number>>()
        number.error shouldBe "expected number but got 'abc' at line 1 column 1"
    }

    @Test
    fun `get optional number returns value`() {
        val rawYaml =
            """
        key1: 123
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val number = result.data.getOptionalNumber("key1").shouldBeTypeOf<Success<Int?>>()
        number.data shouldBe 123
    }

    @Test
    fun `get optional number returns null when key missing`() {
        val rawYaml =
            """
        key2: 123
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val number = result.data.getOptionalNumber("key1").shouldBeTypeOf<Success<Int?>>()
        number.data shouldBe null
    }

    @Test
    fun `get optional number returns error for invalid value`() {
        val rawYaml =
            """
        key1: "abc"
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val number = result.data.getOptionalNumber("key1").shouldBeTypeOf<Error<Int?>>()
        number.error shouldBe "expected number but got 'abc' at line 1 column 1"
    }

    @Test
    fun `get optional string returns value`() {
        val rawYaml =
            """
        key1: "foo"
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val string = result.data.getOptionalString("key1").shouldBeTypeOf<Success<String?>>()
        string.data shouldBe "foo"
    }

    @Test
    fun `get optional string returns null when key missing`() {
        val rawYaml =
            """
        key2: "foo"
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val string = result.data.getOptionalString("key1").shouldBeTypeOf<Success<String?>>()
        string.data shouldBe null
    }

    @Test
    fun `get optional string returns null when value is null`() {
        val rawYaml =
            """
        key1: null
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val string = result.data.getOptionalString("key1").shouldBeTypeOf<Success<String?>>()
        string.data shouldBe null
    }

    @Test
    fun `get optional string with default returns value when key present`() {
        val rawYaml =
            """
        key1: "foo"
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val string = result.data.getOptionalString("key1", "default1").shouldBeTypeOf<Success<String>>()
        string.data shouldBe "foo"
    }

    @Test
    fun `get optional string with default returns default when key missing`() {
        val rawYaml =
            """
        key2: "foo"
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val string = result.data.getOptionalString("key1", "default1").shouldBeTypeOf<Success<String>>()
        string.data shouldBe "default1"
    }

    @Test
    fun `get optional string with default returns default when value is null`() {
        val rawYaml =
            """
        key1: null
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val string = result.data.getOptionalString("key1", "default1").shouldBeTypeOf<Success<String>>()
        string.data shouldBe "default1"
    }

    @Test
    fun `get optional boolean returns null when key missing`() {
        val rawYaml =
            """
        key2: true
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val bool = result.data.getOptionalBoolean("key1").shouldBeTypeOf<Success<Boolean?>>()
        bool.data shouldBe null
    }

    @Test
    fun `get optional boolean returns error for invalid value`() {
        val rawYaml =
            """
        key1: "yolo"
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val bool = result.data.getOptionalBoolean("key1").shouldBeTypeOf<Error<Boolean?>>()
        bool.error shouldBe "expected 'true' or 'false' but got 'yolo' at line 1 column 1"
    }

    @Test
    fun `get boolean with default returns value when key present`() {
        val rawYaml =
            """
        key1: false
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val bool = result.data.getBoolean("key1", true).shouldBeTypeOf<Success<Boolean>>()
        bool.data shouldBe false
    }

    @Test
    fun `get boolean with default returns default when key missing`() {
        val rawYaml =
            """
        key2: false
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val bool = result.data.getBoolean("key1", true).shouldBeTypeOf<Success<Boolean>>()
        bool.data shouldBe true
    }

    @Test
    fun `get boolean with default returns error for invalid value`() {
        val rawYaml =
            """
        key1: "yolo"
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val bool = result.data.getBoolean("key1", true).shouldBeTypeOf<Error<Boolean>>()
        bool.error shouldBe "expected 'true' or 'false' but got 'yolo' at line 1 column 1"
    }

    @Test
    fun `get boolean is case sensitive`() {
        val rawYaml =
            """
        key1: "True"
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val bool = result.data.getBoolean("key1").shouldBeTypeOf<Error<Boolean>>()
        bool.error shouldBe "expected 'true' or 'false' but got 'True' at line 1 column 1"
    }

    @Test
    fun `get string on scalar document returns error`() {
        val result = yamlParse("foo").shouldBeTypeOf<Success<YamlNode>>()
        val string = result.data.getString("key1").shouldBeTypeOf<Error<String>>()
        string.error shouldBe "expected a map, got ''foo''"
    }

    @Test
    fun `get list on scalar document returns error`() {
        val result = yamlParse("foo").shouldBeTypeOf<Success<YamlNode>>()
        val list = result.data.getList("key1").shouldBeTypeOf<Error<YamlList>>()
        list.error shouldBe "expected a list at 'key1' but got ''foo''"
    }

    @Test
    fun `get map on scalar document returns error`() {
        val result = yamlParse("foo").shouldBeTypeOf<Success<YamlNode>>()
        val map = result.data.getMap("key1").shouldBeTypeOf<Error<YamlMap>>()
        map.error shouldBe "expected a map at 'key1' but got ''foo''"
    }

    @Test
    fun `get string map on scalar document returns error`() {
        val result = yamlParse("foo").shouldBeTypeOf<Success<YamlNode>>()
        val map = result.data.getStringMap("key1").shouldBeTypeOf<Error<Map<String, String>>>()
        map.error shouldBe "expected a map at 'key1' but got ''foo''"
    }

    @Test
    fun `get keys on scalar document returns error`() {
        val result = yamlParse("foo").shouldBeTypeOf<Success<YamlNode>>()
        val keys = result.data.getKeys().shouldBeTypeOf<Error<List<String>>>()
        keys.error shouldBe "'foo' is not a map"
    }

    @Test
    fun `get keys returns all keys`() {
        val rawYaml =
            """
        key1: "foo"
        key2: "bar"
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val keys = result.data.getKeys().shouldBeTypeOf<Success<List<String>>>()
        keys.data shouldBe listOf("key1", "key2")
    }

    @Test
    fun `get string returns error when value is a map`() {
        val rawYaml =
            """
        key1:
          item1: string1
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val string = result.data.getString("key1").shouldBeTypeOf<Error<String>>()
        string.error shouldBe "expected string but found '{'item1': 'string1'}'"
    }

    @Test
    fun `get string returns error when value is a list`() {
        val rawYaml =
            """
        key1:
          - string1
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val string = result.data.getString("key1").shouldBeTypeOf<Error<String>>()
        string.error shouldBe "expected string but found '['string1']'"
    }

    @Test
    fun `get non null or empty string returns error when key missing`() {
        val rawYaml =
            """
        key2: "bar"
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val string = result.data.getNonNullOrEmptyString("key1").shouldBeTypeOf<Error<String>>()
        string.error shouldBe "key 'key1' not found at line 1 column 1"
    }

    @Test
    fun `get map string returns value`() {
        val rawYaml =
            """
        key1: "foo"
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        result.data.getMapString("key1") shouldBe "foo"
    }

    @Test
    fun `get map string returns null when key missing`() {
        val rawYaml =
            """
        key2: "foo"
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        result.data.getMapString("key1") shouldBe null
    }

    @Test
    fun `get map string returns null for non scalar value`() {
        val rawYaml =
            """
        key1:
          item1: string1
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        result.data.getMapString("key1") shouldBe null
    }

    @Test
    fun `get map string returns null when node is not a map`() {
        val result = yamlParse("foo").shouldBeTypeOf<Success<YamlNode>>()
        result.data.getMapString("key1") shouldBe null
    }

    @Test
    fun `get string map missing key returns empty`() {
        val rawYaml =
            """
        key2:
          item1: string1
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val map = result.data.getStringMap("key1").shouldBeTypeOf<YamlEmpty<Map<String, String>>>()
        map.message shouldBe "'key1' not set"
    }

    @Test
    fun `get string map returns error when key is a list`() {
        val rawYaml =
            """
        key1:
          - item1: string1
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val map = result.data.getStringMap("key1").shouldBeTypeOf<Error<Map<String, String>>>()
        map.error shouldBe "key 'key1' should be a map line 1 column 1"
    }

    @Test
    fun `get string map returns empty map for empty map`() {
        val rawYaml =
            """
        key1: {}
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val map = result.data.getStringMap("key1").shouldBeTypeOf<Success<Map<String, String>>>()
        map.data.entries.count() shouldBe 0
    }

    @Test
    fun `get string map lists all non string keys`() {
        val rawYaml =
            """
        key1:
          item1:
            - foo: bar
          item2: string2
          item3:
            foo: bar
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val map = result.data.getStringMap("key1").shouldBeTypeOf<Error<Map<String, String>>>()
        map.error shouldBe "found non string value in map 'key1' at key(s) item1, item3"
    }

    @Test
    fun `get list with factory parses all items`() {
        val rawYaml =
            """
        key1:
          - name: name1
          - name: name2
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val list = result.data.getList("key1", ItemFactory()).shouldBeTypeOf<Success<List<Item>>>()
        list.data shouldBe listOf(Item("name1"), Item("name2"))
    }

    @Test
    fun `get list with factory returns empty list when key missing`() {
        val rawYaml =
            """
        key2:
          - name: name1
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val list = result.data.getList("key1", ItemFactory()).shouldBeTypeOf<Success<List<Item>>>()
        list.data shouldHaveSize 0
    }

    @Test
    fun `get list with factory aggregates item errors`() {
        val rawYaml =
            """
        key1:
          - name: name1
          - other: foo
          - other: bar
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val list = result.data.getList("key1", ItemFactory()).shouldBeTypeOf<Error<List<Item>>>()
        list.error shouldBe
            "key 'name' not found at line 3 column 5, key 'name' not found at line 4 column 5"
    }

    @Test
    fun `get object parses map`() {
        val rawYaml =
            """
        key1:
          name: name1
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val obj = result.data.getObject("key1", ItemFactory()).shouldBeTypeOf<Success<Item>>()
        obj.data shouldBe Item("name1")
    }

    @Test
    fun `get object parses empty map when key missing`() {
        val rawYaml =
            """
        key2:
          name: name1
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val obj = result.data.getObject("key1", EntryCountFactory()).shouldBeTypeOf<Success<Int>>()
        obj.data shouldBe 0
    }

    @Test
    fun `get object returns error when key is a list`() {
        val rawYaml =
            """
        key1:
          - name: name1
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val obj = result.data.getObject("key1", ItemFactory()).shouldBeTypeOf<Error<Item>>()
        obj.error shouldBe "key 'key1' should be a map line 1 column 1"
    }

    @Test
    fun `get polymorphic list returns empty list when key missing`() {
        val rawYaml =
            """
        list2:
            - type: type1
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val list =
            result.data
                .getPolymorphicList(
                    "list1",
                    mapOf("type1" to Type1Factory(), "type2" to Type2Factory()),
                )
                .shouldBeTypeOf<Success<List<BaseType>>>()

        list.data shouldHaveSize 0
    }

    @Test
    fun `get polymorphic list returns error when key is not a list`() {
        val rawYaml =
            """
        list1:
            type: type1
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val list =
            result.data
                .getPolymorphicList(
                    "list1",
                    mapOf("type1" to Type1Factory(), "type2" to Type2Factory()),
                )
                .shouldBeTypeOf<Error<List<BaseType>>>()

        list.error shouldBe "key 'list1' should be a list line 1 column 1"
    }

    @Test
    fun `get polymorphic list returns error when item is not a map`() {
        val rawYaml =
            """
        list1:
            - type1
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val list =
            result.data
                .getPolymorphicList(
                    "list1",
                    mapOf("type1" to Type1Factory(), "type2" to Type2Factory()),
                )
                .shouldBeTypeOf<Error<List<BaseType>>>()

        list.error shouldBe "expected a map, got ''type1''"
    }

    @Test
    fun `get polymorphic list aggregates errors`() {
        val rawYaml =
            """
        list1:
            - type: type123
            - type: type456
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val list =
            result.data
                .getPolymorphicList(
                    "list1",
                    mapOf("type1" to Type1Factory(), "type2" to Type2Factory()),
                )
                .shouldBeTypeOf<Error<List<BaseType>>>()

        list.error shouldBe
            "unknown type 'type123', possible types are 'type1', 'type2' at line 2 column 7, " +
            "unknown type 'type456', possible types are 'type1', 'type2' at line 3 column 7"
    }

    @Test
    fun `get polymorphic list with no factories`() {
        val rawYaml =
            """
        list1:
            - type: type1
        """
                .trimIndent()

        val result = yamlParse(rawYaml).shouldBeTypeOf<Success<YamlNode>>()
        val list =
            result.data
                .getPolymorphicList("list1", emptyMap<String, PolymorphicConfigurationFactory<BaseType>>())
                .shouldBeTypeOf<Error<List<BaseType>>>()

        list.error shouldBe "unknown type 'type1', possible types are <none> at line 2 column 7"
    }
}
