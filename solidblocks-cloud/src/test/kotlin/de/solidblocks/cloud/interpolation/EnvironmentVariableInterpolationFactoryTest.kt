package de.solidblocks.cloud.interpolation

import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class EnvironmentVariableInterpolationFactoryTest {

    private val factory = EnvironmentVariableInterpolationFactory()

    @Test
    fun testType() {
        factory.type shouldBe "env"
    }

    @Test
    fun testValidateSimpleVariable() {
        factory.validate("MY_VAR").shouldBeInstanceOf<Success<Unit>>()
    }

    @Test
    fun testValidateVariableWithDefault() {
        factory.validate("MY_VAR:fallback").shouldBeInstanceOf<Success<Unit>>()
    }

    @Test
    fun testValidateVariableWithEmptyDefault() {
        factory.validate("MY_VAR:").shouldBeInstanceOf<Success<Unit>>()
    }

    @Test
    fun testValidateEmpty() {
        val result = factory.validate("").shouldBeInstanceOf<Error<Unit>>()
        result.error shouldBe "invalid empty environment variable reference"
    }

    @Test
    fun testValidateColonOnly() {
        val result = factory.validate(":default").shouldBeInstanceOf<Error<Unit>>()
        result.error shouldBe "invalid empty environment variable reference"
    }

    @Test
    fun testResolveSetVariable() {
        val result = factory.resolve("HOME").shouldBeInstanceOf<Success<String>>()
        result.data shouldBe System.getenv("HOME")
    }

    @Test
    fun testResolveUnsetVariableWithDefault() {
        val result = factory.resolve("MISSING_VAR:fallback").shouldBeInstanceOf<Success<String>>()
        result.data shouldBe "fallback"
    }

    @Test
    fun testResolveUnsetVariableWithEmptyDefault() {
        val result = factory.resolve("MISSING_VAR:").shouldBeInstanceOf<Success<String>>()
        result.data shouldBe ""
    }

    @Test
    fun testResolveUnsetVariableWithoutDefault() {
        val result = factory.resolve("MISSING_VAR").shouldBeInstanceOf<Error<String>>()
        result.error shouldBe "environment variable 'MISSING_VAR' is not set"
    }

    @Test
    fun testResolveSetVariableTakesPrecedenceOverDefault() {
        val result = factory.resolve("HOME:fallback").shouldBeInstanceOf<Success<String>>()
        result.data shouldBe System.getenv("HOME")
    }

    @Test
    fun testResolveDefaultCanContainColon() {
        val result = factory.resolve("MISSING_VAR:host:8080").shouldBeInstanceOf<Success<String>>()
        result.data shouldBe "host:8080"
    }
}
