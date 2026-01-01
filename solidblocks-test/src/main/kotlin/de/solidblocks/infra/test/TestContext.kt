package de.solidblocks.infra.test


open class TestContext(val priority: Int = 0) {

    val testContexts = mutableListOf<TestContext>()

    open fun beforeAll() {
    }

    open fun afterAll() {
    }

}
