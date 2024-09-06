package de.solidblocks.cli.workflow.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import de.solidblocks.cli.commands.TaskCommand
import de.solidblocks.cli.workflow.Workflow
import de.solidblocks.cli.workflow.conditions.conditionFactories
import de.solidblocks.cli.workflow.tasks.taskFactories

val workflowHelp = """

A workflow file defines a set of tasks that can be executed using this command line tool. Tasks can be used to orchestrate arbitrary commands. Task also define input arguments, help and dependencies, turning a workflow file into a developer friendly execution frontend for your project. 

# Format 

A workflow is defined in the YAML format, with the following root syntax:

```
---
requirements: [...]
tasks: [...]
```

see the following paragraphs for a more detailed description of each root key. You can also generate a example workflow file with all available options by calling `blcks workflow example`. 

# Requirements

A list of preconditions that have to be met, before any workflow task can be executed. Checks are run before any task execution, or can manually be invoked using the `blcks workflow check` command.

${conditionFactories.joinToString("\n") { it.help() }}

# Tasks

${taskFactories.joinToString("\n") { it.help() }}

""".trimIndent()

class WorkflowCommand : CliktCommand(
    name = "workflow", help = "Execute tasks from a Solidblocks workflow file", epilog = workflowHelp
) {

    override fun run() {
    }

    companion object {
        fun createFromWorkflow(workflow: Workflow): WorkflowCommand {
            val workflowCommand = WorkflowCommand()
            workflowCommand.subcommands(workflow.tasks.map { TaskCommand(it) })
            workflowCommand.subcommands(WorkflowCheckCommand(workflow))
            workflowCommand.subcommands(WorkflowExampleCommand(workflow))
            return workflowCommand
        }
    }
}
