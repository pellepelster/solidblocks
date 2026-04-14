package de.solidblocks.cli.cloud

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.installMordantMarkdown
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import de.solidblocks.cloud.CloudHelp

class CloudHelpConfigurationHugoCommand : CliktCommand(name = "configuration-hugo") {
    private val hugoFile by argument().file()

    init {
        installMordantMarkdown()
    }

    override fun help(context: Context) = "generate a cloud configuration file documentation for Hugo"

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
