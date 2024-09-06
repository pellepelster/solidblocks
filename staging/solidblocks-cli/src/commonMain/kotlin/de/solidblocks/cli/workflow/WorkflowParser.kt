package de.solidblocks.cli.workflow

import CommandConditionFactory
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.yamlMap
import com.saveourtool.okio.absolute
import de.solidblocks.cli.utils.Empty
import de.solidblocks.cli.utils.Error
import de.solidblocks.cli.utils.Result
import de.solidblocks.cli.utils.Success
import de.solidblocks.cli.utils.aggregateErrors
import de.solidblocks.cli.utils.getKeys
import de.solidblocks.cli.utils.getList
import de.solidblocks.cli.utils.getMapString
import de.solidblocks.cli.utils.hasError
import de.solidblocks.cli.utils.logMessage
import de.solidblocks.cli.utils.mapSuccess
import de.solidblocks.cli.utils.yamlParse
import de.solidblocks.cli.workflow.conditions.Condition
import de.solidblocks.cli.workflow.conditions.FileConditionFactory
import de.solidblocks.cli.workflow.conditions.conditionFactories
import de.solidblocks.cli.workflow.conditions.conditionSingleFactory
import de.solidblocks.cli.workflow.tasks.Command
import de.solidblocks.cli.workflow.tasks.taskFactories
import okio.FileSystem
import okio.Path.Companion.toPath

object WorkflowParser {

    init {
        conditionFactories.add(FileConditionFactory())
        conditionFactories.add(CommandConditionFactory())
        taskFactories.add(Command())
    }

    private val fs = FileSystem.SYSTEM

    val workflowFile = "workflow.yml".toPath(normalize = true).absolute()

    fun workflowExists() = fs.exists(workflowFile)

    fun readWorkflow() = FileSystem.SYSTEM.read(workflowFile) {

        readUtf8()
    }

    fun parse(workflow: String): Result<Workflow> {
        val workflowYaml = when (val result = yamlParse(workflow)) {
            is Error -> return Error("failed to parse workflow file '${workflowFile}' (${result.error})")
            is Empty -> return Error("workflow file '${workflowFile}' was empty")
            is Success -> result.data
        }

        val requirements = when (val result = workflowYaml.getList("requirements")) {
            is Error -> return Error("failed to parse workflow file: ${result.error}")
            is Empty -> emptyList()
            is Success -> parseRequirements(result.data)
        }

        if (requirements.hasError()) {
            return Error(requirements.aggregateErrors())
        }

        val tasks = when (val result = workflowYaml.getList("tasks")) {
            is Error -> return Error("failed to parse workflow file: ${result.error}")
            is Empty -> emptyList()
            is Success -> parseTasks(result.data)
        }

        if (tasks.hasError()) {
            return Error(tasks.aggregateErrors())
        }

        return Success(Workflow(requirements.mapSuccess(), tasks.mapSuccess()))
    }

    private fun parseRequirements(list: YamlList): List<Result<out Condition>> = list.items.map {
        val keys = when (val result = it.getKeys()) {
            is Empty -> return@map Error<Condition>("no keys found at ${list.location.logMessage()}")
            is Error -> return@map Error<Condition>("${result.error} at ${list.location.logMessage()}")
            is Success -> result.data
        }

        val factory = when (val result = conditionSingleFactory(keys)) {
            is Empty -> return@map Error<Condition>("${result.message} at ${list.location.logMessage()}")
            is Error -> return@map Error<Condition>("${result.error} at ${list.location.logMessage()}")
            is Success -> result.data
        }

        factory.parse(it)
    }

    private fun parseTasks(list: YamlList) = list.items.map {
        val name = it.yamlMap.getMapString("name")
        if (name == null) {
            Error("missing 'name' for task definition at ${it.yamlMap.location.logMessage()}")
        } else {
            val help = it.yamlMap.getMapString("help")
            Success(Task(name, help))
        }
    }
}