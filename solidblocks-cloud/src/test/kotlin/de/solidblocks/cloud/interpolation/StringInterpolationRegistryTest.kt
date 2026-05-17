package de.solidblocks.cloud.interpolation

import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import de.solidblocks.shell.systemd.Unit
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class StringInterpolationRegistryTest {

    val registry = StringInterpolationRegistry(listOf(EnvironmentVariableInterpolationFactory()))

    @Test
    fun testValidateInvalid() {
        val result = registry.validate("\${invalid:something}").shouldBeInstanceOf<Error<Unit>>()
        result.error shouldBe "invalid interpolation 'invalid:something', could not resolve type 'invalid'"
    }

    @Test
    fun testValidateEmpty() {
        val result = registry.validate("\${}").shouldBeInstanceOf<Error<Unit>>()
        result.error shouldBe "invalid empty interpolation"
    }

    @Test
    fun testResolveEmpty() {
        val result = registry.resolve("").shouldBeInstanceOf<Success<Unit>>()
        result.data shouldBe ""
    }

    @Test
    fun testValidateEnvVariable() {
        val result = registry.validate("\${env:HOME}").shouldBeInstanceOf<Success<Unit>>()
        result.data shouldBe kotlin.Unit
    }

    @Test
    fun testValidateEnvVariableEmpty() {
        val result = registry.validate("\${env:}").shouldBeInstanceOf<Error<Unit>>()
        result.error shouldBe "invalid empty environment variable reference"
    }

    @Test
    fun testResolveEnvVariableEmpty() {
        val result = registry.resolve("\${env:}").shouldBeInstanceOf<Error<Unit>>()
        result.error shouldBe "invalid empty environment variable reference"
    }

    @Test
    fun testResolveEnvVariable() {
        val result = registry.resolve("aa\${env:HOME}bb").shouldBeInstanceOf<Success<String>>()
        result.data shouldBe "aa${System.getenv("HOME")}bb"
    }
}
