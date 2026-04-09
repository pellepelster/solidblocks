package de.solidblocks.cloud

import de.solidblocks.cloud.mocks.MockResource1
import de.solidblocks.cloud.mocks.MockResource1Lookup
import de.solidblocks.cloud.mocks.MockResource2Lookup
import de.solidblocks.cloud.utils.formatBytes
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class UtilsTest {

    @Test
    fun testLogText() {
        MockResource1("resource1").logText() shouldBe "mockresource1 'resource1'"
        MockResource1Lookup("resource1").logText() shouldBe "mockresource1 'resource1'"
        MockResource2Lookup("resource2").logText() shouldBe "custom log text 'resource2'"
    }

    @Test
    fun testFormatBytes() {
        0L.formatBytes() shouldBe "0 B"
        512L.formatBytes() shouldBe "512 B"
        1024L.formatBytes() shouldBe "1.00 KB"
        1_536_000L.formatBytes() shouldBe "1.46 MB"
        2_147_483_648.formatBytes() shouldBe "2.00 GB"
    }
}
