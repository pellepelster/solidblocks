package de.solidblocks.cli.workflow.commands

import com.github.ajalt.clikt.core.CliktCommand
import de.solidblocks.cli.workflow.Workflow
import de.solidblocks.cli.workflow.conditions.conditionFactories
import de.solidblocks.cli.workflow.tasks.taskFactories

class WorkflowExampleCommand(private val workflow: Workflow) : CliktCommand(
    name = "example",
    help = "Generate an example workflow file with all available options",
) {

    override fun run() {
        val sb = StringBuilder()
        sb.appendLine("---")
        sb.appendLine("# workflow example file ")
        sb.appendLine("requirements:")
        sb.appendLine()
        conditionFactories.forEach {
            sb.appendLine(it.helpExample(currentContext.terminal).prependIndent("    "))
        }
        sb.appendLine()
        sb.appendLine()

        sb.appendLine("tasks:")
        sb.appendLine()
        taskFactories.forEach {
            sb.appendLine(it.helpExample(currentContext.terminal).prependIndent("    "))
        }

        println(sb.toString())
    }
}
