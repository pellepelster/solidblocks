package de.solidblocks.cli.workflow.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple

class WorkflowErrorCommand(message: String) : CliktCommand(
    name = "workflow", help = message,
    invokeWithoutSubcommand = true,
    treatUnknownOptionsAsArgs = true,
) {

    //TODO hide arg in help output to not consume users, this is only needed to allow arbitrary workflow ... calls when no file is present
    val arguments by argument().multiple()

    override fun run() {
        throw PrintHelpMessage(this.currentContext)
    }
}
