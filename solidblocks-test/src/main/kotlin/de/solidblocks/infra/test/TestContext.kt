package de.solidblocks.infra.test

import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

@OptIn(ExperimentalAtomicApi::class) val testContextOrder = AtomicLong(0)

open class TestContext {

  @OptIn(ExperimentalAtomicApi::class) val order = testContextOrder.incrementAndFetch()

  val testContexts = mutableListOf<TestContext>()

  open fun beforeAll() {}

  open fun afterAll() {}

  open fun cleanUp() {}
}
