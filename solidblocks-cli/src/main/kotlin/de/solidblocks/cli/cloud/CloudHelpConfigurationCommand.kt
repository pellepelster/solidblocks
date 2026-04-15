package de.solidblocks.cli.cloud

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.installMordantMarkdown
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.mordant.markdown.Markdown
import de.solidblocks.cli.utils.createTerminal
import de.solidblocks.cloud.CloudConfigurationHelp

class CloudHelpConfigurationCommand : CliktCommand(name = "configuration") {

    // private val argument by argument("").file().optional()

    enum class HelpFormat {
        console,
        hugo,
        `json-schema`,
    }

    val format by option().choice("console", "hugo", "json-schema").enum<HelpFormat>(true).default(HelpFormat.console)

    init {
        installMordantMarkdown()
    }

    override fun help(context: Context) = "Cloud configuration file documentation"

    override fun run() {
        when (format) {
            HelpFormat.console -> {
                val terminal = createTerminal()
                val md = Markdown(CloudConfigurationHelp().renderMarkdown(false), true, false)
                terminal.println(md)
            }

            HelpFormat.hugo -> {
                val hugo = """
                    +++
                    title = 'Configuration'
                    description = 'configuration file format documentation'
                    +++
                    ${CloudConfigurationHelp().renderMarkdown(true)}
                """.trimIndent()
                println(hugo)
            }

            HelpFormat.`json-schema` -> {
                println(CloudConfigurationHelp().renderJsonSchema())
            }
        }
    }
}
