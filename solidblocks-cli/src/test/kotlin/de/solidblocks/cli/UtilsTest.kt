package de.solidblocks.cli

import de.solidblocks.cli.hetzner.api.resources.FloatingIpResponse
import de.solidblocks.cli.utils.pascalCaseToWhiteSpace
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UtilsTest {

  @Test
  fun pascalCaseToWhiteSpace() {
    assertEquals("floating ip", FloatingIpResponse::class.pascalCaseToWhiteSpace())
  }
}
