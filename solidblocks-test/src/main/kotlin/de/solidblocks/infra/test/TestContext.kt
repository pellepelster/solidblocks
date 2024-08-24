package de.solidblocks.infra.test

import java.nio.file.Path

interface TestContext<T : CommandBuilder> {
    fun command(executable: Path): T
}