package de.solidblocks.infra.test

import java.nio.file.Path

interface TestContext {
    fun command(executable: Path): CommandBuilder
}