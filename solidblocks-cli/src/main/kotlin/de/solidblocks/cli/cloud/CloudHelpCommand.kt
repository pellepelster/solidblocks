package de.solidblocks.cli.cloud

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.installMordantMarkdown

class CloudHelpCommand : CliktCommand(name = "help") {

  init {
    installMordantMarkdown()
  }

  override fun help(context: Context) = "Solidblocks cloud configuration file documentation\n"

  // + DocumentationGenerator(CloudConfigurationFactory(emptyList(),
  // emptyList())).generateMarkdown()

  override fun run() {}
}
