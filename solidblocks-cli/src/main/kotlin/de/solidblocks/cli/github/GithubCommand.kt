package de.solidblocks.cli.github

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context

class GithubCommand : CliktCommand(name = "github") {

  override fun help(context: Context) = "Github automation utilities"

  override fun run() {}
}
