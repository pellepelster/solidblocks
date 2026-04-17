package de.solidblocks.infra.test.local

import de.solidblocks.infra.test.command.CommandResult
import de.solidblocks.infra.test.output.TimestampedOutputLine
import de.solidblocks.infra.test.script.ScriptBuilder
import kotlinx.coroutines.runBlocking
import localTestContext
import kotlin.time.Duration

class LocalScriptBuilder(timeout: Duration) : ScriptBuilder(timeout) {
    override fun run(): CommandResult<TimestampedOutputLine> = runBlocking {
        val buildScript = buildScript()
        val command =
            localTestContext(timeout = timeout)
                .command(*buildScript.second.toTypedArray())
                .workingDir(buildScript.first)
                .env(envs)
                .inheritEnv(inheritEnv)
                .timeout(timeout)

        if (assertSteps) {
            steps.forEachIndexed { index, step ->
                command.assert { it.waitForOutput(".*finished step $index.*") { "continue" } }

                command.assert { step.assertion?.invoke(it) }
            }
        }

        command.runResult()
    }
}
