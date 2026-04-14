package de.solidblocks.cli.cloud

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.installMordantMarkdown
import com.github.ajalt.mordant.markdown.Markdown
import de.solidblocks.cli.utils.createTerminal
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.logError

class CloudHelpCommand : CliktCommand(name = "help") {

    init {
        installMordantMarkdown()
    }

    override fun help(context: Context) = "Solidblocks cloud help"

    override fun run() {
    }

    companion object {
        public fun printMarkdown(markdown: Result<String>) {
            val terminal = createTerminal()
            when (markdown) {
                is Error<String> -> {
                    logError(markdown.error)
                    throw ProgramResult(1)
                }

                is Success<String> -> {
                    terminal.println(
                        Markdown(
                            markdown.data,
                            true,
                            false,
                        ),
                    )
                }
            }
        }
    }
}
