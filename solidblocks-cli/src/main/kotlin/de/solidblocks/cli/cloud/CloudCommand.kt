package de.solidblocks.cli.cloud

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context

class CloudCommand : CliktCommand(name = "cloud") {
    override fun help(context: Context) = "manage Solidblocks cloud environments"

    override fun run() {}
}
