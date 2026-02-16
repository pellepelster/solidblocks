package de.solidblocks.cli.cloud

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context

class CloudCommand : CliktCommand(name = "cloud") {

  override fun help(context: Context) =
      "Manage Solidblocks cloud environments, for help on the configuration file please run **blcks cloud help**"

  override fun run() {}
}
