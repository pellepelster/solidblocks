package de.solidblocks.infra.test

open class TestContext {

  val testContexts = mutableListOf<TestContext>()

  open fun beforeAll() {
    testContexts.forEach { it.beforeAll() }
  }

  open fun afterAll() {
    testContexts.forEach { it.afterAll() }
  }
}
