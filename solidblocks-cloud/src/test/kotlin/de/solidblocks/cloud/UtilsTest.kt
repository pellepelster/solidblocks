package de.solidblocks.cloud

import de.solidblocks.cloud.mocks.MockResource1
import de.solidblocks.cloud.mocks.MockResource1Lookup
import de.solidblocks.cloud.mocks.MockResource2Lookup
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