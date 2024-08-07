package de.solidblocks.infra.test.ssh

import de.solidblocks.infra.test.CommandBuilder
import de.solidblocks.infra.test.CommandRunResult
import de.solidblocks.infra.test.TestContext
import java.nio.file.Path


class SshCommandBuilder : CommandBuilder("yyy") {
    override fun runInternal(): CommandRunResult {
        TODO("Not yet implemented")
    }
}

class SshTestContext : TestContext {
    override fun command(executable: Path) = SshCommandBuilder()
}

fun ssh(): TestContext = SshTestContext()
