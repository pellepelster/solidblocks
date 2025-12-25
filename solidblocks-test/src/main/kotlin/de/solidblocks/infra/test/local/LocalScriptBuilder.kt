package de.solidblocks.infra.test.local

import de.solidblocks.infra.test.command.CommandResult
import de.solidblocks.infra.test.output.TimestampedOutputLine
import de.solidblocks.infra.test.script.ScriptBuilder
import kotlinx.coroutines.runBlocking
import localTestContext

class LocalScriptBuilder : ScriptBuilder() {

  override fun run(): CommandResult<TimestampedOutputLine> = runBlocking {
    val buildScript = buildScript()
    val command =
        localTestContext()
            .command(*buildScript.second.toTypedArray())
            .workingDir(buildScript.first)
            .env(envs)
            .inheritEnv(inheritEnv)
            .defaultWaitForOutput(defaultWaitForOutput)

    if (assertSteps) {
      steps.forEachIndexed { index, step ->
        command.assert { it.waitForOutput(".*finished step $index.*") { "continue" } }

        command.assert { step.assertion?.invoke(it) }
      }
    }

    command.runResult()
  }
}
