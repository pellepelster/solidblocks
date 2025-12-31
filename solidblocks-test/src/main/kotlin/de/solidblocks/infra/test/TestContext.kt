package de.solidblocks.infra.test

interface TestContext {
  fun beforeAll() {}

  fun afterAll() {}

  fun cleanup() {}
}
