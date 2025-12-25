package de.solidblocks.infra.test.assertions

import de.solidblocks.infra.test.host.HostTestContext
import io.kotest.matchers.shouldBe

infix fun HostTestContext.portShouldBeOpen(port: Int): HostTestContext {
  this.portIsOpen(port) shouldBe true
  return this
}

infix fun HostTestContext.portShouldBeClosed(port: Int): HostTestContext {
  this.portIsOpen(port) shouldBe false
  return this
}
