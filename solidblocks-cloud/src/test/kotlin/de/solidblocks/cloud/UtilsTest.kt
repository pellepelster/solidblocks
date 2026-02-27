package de.solidblocks.cloud

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class UtilsTest {
    @Test
    fun testLogText() {
        MockResource1("resource1").logText() shouldBe "mockresource1 'resource1'"
        MockResource1Lookup("resource1").logText() shouldBe "mockresource1 'resource1'"
        MockResource2Lookup("resource2").logText() shouldBe "custom log text 'resource2'"
    }
}