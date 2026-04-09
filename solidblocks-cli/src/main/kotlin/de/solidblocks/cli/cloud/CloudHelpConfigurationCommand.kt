package de.solidblocks.cli.cloud

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.installMordantMarkdown
import com.github.ajalt.mordant.markdown.Markdown
import de.solidblocks.cli.utils.createTerminal
import de.solidblocks.cloud.CloudHelp

class CloudHelpConfigurationCommand : CliktCommand(name = "configuration") {
    init {
        installMordantMarkdown()
    }

    override fun help(context: Context) = "Cloud configuration file documentation"

    override fun run() {
        val terminal = createTerminal()

        val md = Markdown(CloudHelp().renderMarkdown(false), true, false)
        terminal.println(md)
    }
}
