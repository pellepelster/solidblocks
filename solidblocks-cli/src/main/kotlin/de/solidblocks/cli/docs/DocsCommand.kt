package de.solidblocks.cli.docs

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context

class DocsCommand : CliktCommand(name = "docs") {

  override fun help(context: Context) = "utilities for documentation generation"

  override fun run() {}
}
