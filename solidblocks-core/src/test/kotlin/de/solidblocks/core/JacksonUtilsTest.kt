package de.solidblocks.core

import com.fasterxml.jackson.annotation.JsonProperty
import de.solidblocks.core.utils.JacksonUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JacksonUtilsTest {

    data class SerializationTest(
        @JsonProperty("my_property")
        val myProperty: String
    )

    @Test
    fun testSerializeToMap() {
        val test = SerializationTest("abc")
        val map = JacksonUtils.toMap(test)
        assertThat(map["my_property"]).isEqualTo("abc")
    }
}
