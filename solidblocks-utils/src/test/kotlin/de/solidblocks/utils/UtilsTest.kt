package de.solidblocks.utils

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class UtilsTest {
    @Test
    fun testLogFormats() {
        "aaabbbccc".sha256Hash() shouldBe "fb84a45f6df7d1d17036f939f1cfeb87339ff5dbdf411222f3762dd76779a287"
    }
}
