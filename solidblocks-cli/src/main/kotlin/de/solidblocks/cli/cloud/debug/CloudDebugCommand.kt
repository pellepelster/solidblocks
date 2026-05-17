package de.solidblocks.cli.cloud.debug

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context

class CloudDebugCommand : CliktCommand(name = "debug") {
    override fun help(context: Context) = "debug a clouds state and config"

    override fun run() {
    }
}
