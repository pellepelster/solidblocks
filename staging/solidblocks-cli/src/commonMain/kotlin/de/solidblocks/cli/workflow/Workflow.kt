package de.solidblocks.cli.workflow

import de.solidblocks.cli.workflow.conditions.Condition

data class Task(val name: String, val help: String? = null)

data class Workflow(val requirements: List<Condition>, val tasks: List<Task>)
