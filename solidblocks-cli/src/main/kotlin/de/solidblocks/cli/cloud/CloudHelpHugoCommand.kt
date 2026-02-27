package de.solidblocks.cli.cloud

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.installMordantMarkdown
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import de.solidblocks.cloud.CloudHelp

class CloudHelpHugoCommand : CliktCommand(name = "help-hugo") {

  private val hugoFile by argument().file()

  init {
    installMordantMarkdown()
  }

  override fun help(context: Context) = "Solidblocks cloud configuration file documentation"

  override fun run() {
    hugoFile.writeText(
        """
+++
title = 'Configuration'
description = 'configuration file format documentation'
+++
${CloudHelp().renderMarkdown(true)}
"""
            .trimIndent(),
    )
  }
}
