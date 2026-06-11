package de.solidblocks.cloud.interpolation

import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class EnvironmentVariableInterpolationFactoryTest {

    private val factory = EnvironmentVariableInterpolationFactory()

    @Test
    fun `type`() {
        factory.interpolationType shouldBe "env"
    }

    @Test
    fun `validate simple variable`() {
        factory.validate("MY_VAR").shouldBeInstanceOf<Success<Unit>>()
    }

    @Test
    fun `validate variable with default`() {
        factory.validate("MY_VAR:fallback").shouldBeInstanceOf<Success<Unit>>()
    }

    @Test
    fun `validate variable with empty default`() {
        factory.validate("MY_VAR:").shouldBeInstanceOf<Success<Unit>>()
    }

    @Test
    fun `validate empty`() {
        val result = factory.validate("").shouldBeInstanceOf<Error<Unit>>()
        result.error shouldBe "invalid empty environment variable reference"
    }

    @Test
    fun `validate colon only`() {
        val result = factory.validate(":default").shouldBeInstanceOf<Error<Unit>>()
        result.error shouldBe "invalid empty environment variable reference"
    }

    @Test
    fun `resolve set variable`() {
        val result = factory.resolve("HOME").shouldBeInstanceOf<Success<String>>()
        result.data shouldBe System.getenv("HOME")
    }

    @Test
    fun `resolve unset variable with default`() {
        val result = factory.resolve("MISSING_VAR:fallback").shouldBeInstanceOf<Success<String>>()
        result.data shouldBe "fallback"
    }

    @Test
    fun `resolve unset variable with empty default`() {
        val result = factory.resolve("MISSING_VAR:").shouldBeInstanceOf<Success<String>>()
        result.data shouldBe ""
    }

    @Test
    fun `resolve unset variable without default`() {
        val result = factory.resolve("MISSING_VAR").shouldBeInstanceOf<Error<String>>()
        result.error shouldBe "environment variable 'MISSING_VAR' is not set"
    }

    @Test
    fun `resolve set variable takes precedence over default`() {
        val result = factory.resolve("HOME:fallback").shouldBeInstanceOf<Success<String>>()
        result.data shouldBe System.getenv("HOME")
    }

    @Test
    fun `resolve default can contain colon`() {
        val result = factory.resolve("MISSING_VAR:host:8080").shouldBeInstanceOf<Success<String>>()
        result.data shouldBe "host:8080"
    }
}
