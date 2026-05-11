package de.solidblocks.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import de.solidblocks.cloud.utils.solidblocksVersion

class VersionCommand : CliktCommand("version") {
    override fun help(context: Context) = "shows the Solidblocks version"

    override fun run() {
        println(solidblocksVersion())
    }
}
