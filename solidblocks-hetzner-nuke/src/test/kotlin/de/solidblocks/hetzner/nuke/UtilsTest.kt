package de.solidblocks.hetzner.nuke

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class UtilsTest {

  // TODO
  @Test
  @Disabled
  fun testValidLabelParameter() {
    validLabelParameter("a=b") shouldBe true
    validLabelParameter("") shouldBe false
    validLabelParameter("a") shouldBe false
    validLabelParameter("a=") shouldBe false
    validLabelParameter("=b") shouldBe false
    validLabelParameter("=") shouldBe false
  }
}
