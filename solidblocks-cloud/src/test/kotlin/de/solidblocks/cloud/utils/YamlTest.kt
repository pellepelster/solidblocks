package de.solidblocks.cloud.utils

import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
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
}
