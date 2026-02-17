package de.solidblocks.cloud

import de.solidblocks.cloud.provisioner.mock.Resource1
import de.solidblocks.cloud.provisioner.mock.Resource1Lookup
import de.solidblocks.cloud.provisioner.mock.Resource2Lookup
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class UtilsTest {
  @Test
  fun testLogText() {
    Resource1("resource1").logText() shouldBe "resource1 'resource1'"
    Resource1Lookup("resource1").logText() shouldBe "resource1 'resource1'"
    Resource2Lookup("resource2").logText() shouldBe "custom log text 'resource2'"
  }
}
