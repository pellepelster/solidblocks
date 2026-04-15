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
import de.solidblocks.utils.logInfo

class CloudHelpConfigurationCommand : CliktCommand(name = "configuration") {

    private val target by argument("target").file().optional()

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
                val configurationHelp = CloudConfigurationHelp().renderMarkdown(false)
                if (target != null) {
                    logInfo("writing help to '${target!!.absolutePath}''")
                    target!!.writeText(configurationHelp)
                } else {
                    val terminal = createTerminal()
                    val md = Markdown(configurationHelp, true, false)
                    terminal.println(md)
                }
            }

            HelpFormat.hugo -> {
                val hugo = """
+++
title = 'Configuration'
description = 'configuration file format documentation'
+++
${CloudConfigurationHelp().renderMarkdown(true)}
                """.trimIndent()

                if (target != null) {
                    logInfo("writing help to '${target!!.absolutePath}''")
                    target!!.writeText(hugo)
                } else {
                    println(hugo)
                }
            }

            HelpFormat.`json-schema` -> {
                val schema = CloudConfigurationHelp().renderJsonSchema()
                if (target != null) {
                    logInfo("writing help to '${target!!.absolutePath}''")
                    target!!.writeText(schema)
                } else {
                    println(schema)
                }
            }
        }
    }
}
