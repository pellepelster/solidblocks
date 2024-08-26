package de.solidblocks.infra.test

interface TestContext<T : CommandBuilder> {
    fun command(vararg command: String): T
}