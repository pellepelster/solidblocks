package de.solidblocks.cli.workflow.tasks

import de.solidblocks.cli.utils.Error
import de.solidblocks.cli.utils.Result
import de.solidblocks.cli.utils.Success
import de.solidblocks.cli.workflow.YamlNodeFactory

interface TaskExecutor {
    fun run()
}

val taskExecutorFactories = mutableListOf<YamlNodeFactory<TaskExecutor>>()

fun conditionSingleFactory(keywords: List<String>): Result<YamlNodeFactory<TaskExecutor>> {

    val factories = keywords.flatMap { k -> taskExecutorFactories.filter { it.keyword.name == k } }

    if (factories.isEmpty()) {
        return Error("no condition found")
    }

    if (factories.size > 1) {
        return Error("found multiple conditions")
    }

    return Success(factories.first())
}
