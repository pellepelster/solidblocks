package de.solidblocks.infra.test

import de.solidblocks.infra.test.command.CommandBuilder

interface TestContext<T : CommandBuilder> {
    fun command(vararg command: String): T
}