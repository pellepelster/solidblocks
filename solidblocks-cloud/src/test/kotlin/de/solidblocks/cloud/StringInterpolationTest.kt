package de.solidblocks.cloud

import de.solidblocks.cloud.utils.containsInterpolation
import de.solidblocks.cloud.utils.interpolateString
import de.solidblocks.cloud.utils.validateInterpolationTemplate
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class StringInterpolationTest {

    private val resolver: (String) -> String = mapOf("foo" to "yolo", "bar" to "123")::getValue

    @Test
    fun testBasicInterpolation() {
        interpolateString("some \${foo}, and then some \${bar}", resolver).value shouldBe "some yolo, and then some 123"
    }

    @Test
    fun testNoPlaceholders() {
        interpolateString("plain string", resolver).value shouldBe "plain string"
    }

    @Test
    fun testEscapedPlaceholder() {
        interpolateString("\\\${foo} is not replaced", resolver).value shouldBe "\${foo} is not replaced"
    }

    @Test
    fun testMixedEscapedAndReal() {
        interpolateString("\\\${foo} but \\\${bar} is real: \\\${foo}", resolver).value shouldBe
            "\${foo} but \${bar} is real: \${foo}"
    }

    @Test
    fun testEscapeDoesNotAffectNeighbor() {
        interpolateString("\\\${foo}\\\${bar}", resolver).value shouldBe "\${foo}\${bar}"
    }

    @Test
    fun testPlaceholderAtStart() {
        interpolateString("\${foo} at start", resolver).value shouldBe "yolo at start"
    }

    @Test
    fun testPlaceholderAtEnd() {
        interpolateString("at end \${foo}", resolver).value shouldBe "at end yolo"
    }

    @Test
    fun testOnlyPlaceholder() {
        interpolateString("\${foo}", resolver).value shouldBe "yolo"
    }

    @Test
    fun testValidationUnclosedBrace() {
        val result = interpolateString("hello \${foo", resolver)
        result.value shouldBe null
        result.error shouldNotBe null
    }

    @Test
    fun testValidationNestedBrace() {
        val result = interpolateString("hello \${foo\${bar}}", resolver)
        result.value shouldBe null
        result.error shouldNotBe null
    }

    @Test
    fun testValidationValid() {
        validateInterpolationTemplate("some \${foo}, and then some \${bar}") shouldBe null
    }

    @Test
    fun testValidationEmpty() {
        validateInterpolationTemplate("") shouldBe null
    }

    @Test
    fun testValidationEscapeIsValid() {
        validateInterpolationTemplate("\\\${foo}") shouldBe null
    }

    @Test
    fun testValidationUnclosed() {
        validateInterpolationTemplate("\${foo") shouldNotBe null
    }

    @Test
    fun testContainsInterpolation() {
        containsInterpolation("some \${foo} here") shouldBe true
        containsInterpolation("\${foo}") shouldBe true
        containsInterpolation("no placeholders") shouldBe false
        containsInterpolation("") shouldBe false
        containsInterpolation("\\\${foo}") shouldBe false
        containsInterpolation("\\\${foo} but \${bar}") shouldBe true
    }
}
