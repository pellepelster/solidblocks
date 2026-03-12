package de.solidblocks.cloud

import de.solidblocks.cloud.Constants.DEFAULT_NETWORK
import de.solidblocks.cloud.Constants.NETWORK_BIT_SHIFT
import de.solidblocks.cloud.Utils.nextNetwork
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

    @Test
    fun testNextNetworks() {
        val network1 = nextNetwork(DEFAULT_NETWORK, NETWORK_BIT_SHIFT)!!
        network1 shouldBe "10.0.0.0/24"

        val network2 = nextNetwork(DEFAULT_NETWORK, NETWORK_BIT_SHIFT, setOf(network1))!!
        network2 shouldBe "10.0.1.0/24"

        val network3 = nextNetwork(DEFAULT_NETWORK, NETWORK_BIT_SHIFT, setOf(network1, network2))
        network3 shouldBe "10.0.2.0/24"
    }
}