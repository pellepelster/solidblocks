package de.solidblocks.infra.test.docker

import de.solidblocks.infra.test.command.CommandResult
import de.solidblocks.infra.test.output.TimestampedOutputLine
import de.solidblocks.infra.test.script.ScriptBuilder

class DockerScriptBuilder(private val image: DockerTestImage) : ScriptBuilder() {

  override fun run(): CommandResult<TimestampedOutputLine> {
    val buildScript = buildScript()
    val command =
        dockerTestContext(image)
            .command(*buildScript.second.toTypedArray())
            .sourceDir(buildScript.first)
            .workingDir(buildScript.first)
            .env(envs)

    if (assertSteps) {
      steps.forEachIndexed { index, step ->
        command.assert { it.waitForOutput(".*finished step $index.*") { "continue" } }
        command.assert { step.assertion?.invoke(it) }
      }
    }

    return command.runResult()
  }
}
