package de.solidblocks.hetzner.cloud

import de.solidblocks.hetzner.cloud.model.pascalCaseToWhiteSpace
import de.solidblocks.hetzner.cloud.resources.FloatingIpResponse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class UtilsTest {

  @Test
  fun pascalCaseToWhiteSpace() {
    Assertions.assertEquals("floating ip", FloatingIpResponse::class.pascalCaseToWhiteSpace())
  }
}
