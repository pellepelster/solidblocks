package de.solidblocks.cli.cloud

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.installMordantMarkdown
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.mordant.markdown.Markdown
import de.solidblocks.cli.utils.createTerminal
import de.solidblocks.cloud.CloudHelp
import de.solidblocks.cloud.CloudManager
import de.solidblocks.cloud.Output
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.logError

class CloudHelpCommand : CliktCommand(name = "help") {

    private val configFile by argument().file().optional()

    init {
        installMordantMarkdown()
    }

    override fun help(context: Context) = "Solidblocks cloud configuration file documentation"

    override fun run() {
        val terminal = createTerminal()

        if (configFile != null) {
            val manager = CloudManager(configFile!!)
            val runtime =
                when (val result = manager.validate()) {
                    is Error<CloudConfigurationRuntime> -> {
                        logError(result.error)
                        throw ProgramResult(1)
                    }

                    is Success<CloudConfigurationRuntime> -> result.data
                }

            terminal.println()
            printHelp(manager.help(runtime))
        } else {
            val md = Markdown(CloudHelp().renderMarkdown(false), true, false)
            terminal.println(md)
        }
    }

    companion object {
        public fun printHelp(help: Result<List<Output>>) {
            val terminal = createTerminal()
            when (help) {
                is Error<List<Output>> -> {
                    logError(help.error)
                    throw ProgramResult(1)
                }

                is Success<List<Output>> -> {
                    help.data.forEach {
                        terminal.println()
                        terminal.println(
                            Markdown(
                                """
# ${it.title}
${it.text}
"""
                                    .trimIndent(),
                                true,
                                false,
                            ),
                        )
                        terminal.println()
                    }
                }
            }
        }
    }
}
