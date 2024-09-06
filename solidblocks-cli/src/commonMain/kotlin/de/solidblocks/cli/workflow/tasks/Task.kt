package de.solidblocks.cli.workflow.tasks

import de.solidblocks.cli.workflow.YamlNodeFactory

interface TaskFactory : YamlNodeFactory<Any>

val taskFactories = mutableListOf<TaskFactory>()
