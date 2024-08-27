package de.solidblocks.infra.test.local

import de.solidblocks.infra.test.command.CommandRunResult
import de.solidblocks.infra.test.script.ScriptBuilder
import kotlinx.coroutines.runBlocking
import testLocal

class LocalScriptBuilder : ScriptBuilder() {

    override fun run(): CommandRunResult = runBlocking {
        val buildScript = buildScript()
        val command = testLocal().command(*buildScript.second.toTypedArray())
            .workingDir(buildScript.first)
            .env(envs)
            .defaultWaitForOutput(defaultWaitForOutput)

        if (assertSteps) {
            steps.forEachIndexed { index, step ->
                command.assert {
                    it.waitForOutput(".*finished step ${index}.*") {
                        "continue"
                    }
                }

                command.assert {
                    step.assertion?.invoke(it)
                }
            }
        }

        command.runResult()
    }

}