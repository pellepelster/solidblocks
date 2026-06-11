package de.solidblocks.cloud.interpolation

import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class StringInterpolationParserTest {

    private val env = mapOf("foo" to "yolo", "bar" to "123")
    private val resolver: (String) -> Success<String> = { key -> Success(env.getValue(key)) }

    @Test
    fun `basic interpolation`() {
        replaceInterpolatedStrings("some \${foo}, and then some \${bar}", resolver)
            .shouldBeInstanceOf<Success<String>>().data shouldBe "some yolo, and then some 123"
    }

    @Test
    fun `no placeholders`() {
        replaceInterpolatedStrings("plain string", resolver)
            .shouldBeInstanceOf<Success<String>>().data shouldBe "plain string"
    }

    @Test
    fun `escaped placeholder`() {
        replaceInterpolatedStrings("\\\${foo} is not replaced", resolver)
            .shouldBeInstanceOf<Success<String>>().data shouldBe "\${foo} is not replaced"
    }

    @Test
    fun `mixed escaped and real`() {
        replaceInterpolatedStrings("\\\${foo} but \\\${bar} is real: \\\${foo}", resolver)
            .shouldBeInstanceOf<Success<String>>().data shouldBe "\${foo} but \${bar} is real: \${foo}"
    }

    @Test
    fun `escape does not affect neighbor`() {
        replaceInterpolatedStrings("\\\${foo}\\\${bar}", resolver)
            .shouldBeInstanceOf<Success<String>>().data shouldBe "\${foo}\${bar}"
    }

    @Test
    fun `placeholder at start`() {
        replaceInterpolatedStrings("\${foo} at start", resolver)
            .shouldBeInstanceOf<Success<String>>().data shouldBe "yolo at start"
    }

    @Test
    fun `placeholder at end`() {
        replaceInterpolatedStrings("at end \${foo}", resolver)
            .shouldBeInstanceOf<Success<String>>().data shouldBe "at end yolo"
    }

    @Test
    fun `only placeholder`() {
        replaceInterpolatedStrings("\${foo}", resolver)
            .shouldBeInstanceOf<Success<String>>().data shouldBe "yolo"
    }

    @Test
    fun `validation unclosed brace`() {
        replaceInterpolatedStrings("hello \${foo", resolver)
            .shouldBeInstanceOf<Error<String>>()
    }

    @Test
    fun `validation nested brace`() {
        replaceInterpolatedStrings("hello \${foo\${bar}}", resolver)
            .shouldBeInstanceOf<Error<String>>()
    }

    @Test
    fun `resolver error`() {
        val failingResolver: (String) -> Error<String> = { key -> Error("unknown key '$key'") }
        replaceInterpolatedStrings("\${foo}", failingResolver)
            .shouldBeInstanceOf<Error<String>>().error shouldBe "unknown key 'foo'"
    }

    @Test
    fun `validation valid`() {
        validateInterpolatedString("some \${foo}, and then some \${bar}") shouldBe null
    }

    @Test
    fun `validation empty`() {
        validateInterpolatedString("") shouldBe null
    }

    @Test
    fun `validation escape is valid`() {
        validateInterpolatedString("\\\${foo}") shouldBe null
    }

    @Test
    fun `validation unclosed`() {
        validateInterpolatedString("\${foo") shouldNotBe null
    }

    @Test
    fun `contains interpolation`() {
        stringContainsInterpolation("some \${foo} here") shouldBe true
        stringContainsInterpolation("\${foo}") shouldBe true
        stringContainsInterpolation("no placeholders") shouldBe false
        stringContainsInterpolation("") shouldBe false
        stringContainsInterpolation("\\\${foo}") shouldBe false
        stringContainsInterpolation("\\\${foo} but \${bar}") shouldBe true
    }
}
