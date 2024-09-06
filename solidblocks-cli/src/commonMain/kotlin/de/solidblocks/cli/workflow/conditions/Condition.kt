package de.solidblocks.cli.workflow.conditions

import de.solidblocks.cli.utils.Error
import de.solidblocks.cli.utils.Result
import de.solidblocks.cli.utils.Success
import de.solidblocks.cli.workflow.YamlNodeFactory

data class CheckResult(val message: String, val success: Boolean)

interface Condition {

    fun check(): CheckResult
}

val conditionFactories = mutableListOf<YamlNodeFactory<Condition>>()

fun conditionSingleFactory(keywords: List<String>): Result<YamlNodeFactory<Condition>> {
    val factories = keywords.flatMap { k -> conditionFactories.filter { it.keyword.name == k } }

    if (factories.isEmpty()) {
        return Error("no requirement definition found")
    }

    if (factories.size > 1) {
        return Error("found multiple requirement definitions")
    }

    return Success(factories.first())
}
