package de.solidblocks.infra.test.script

import de.solidblocks.infra.test.command.CommandResult
import de.solidblocks.infra.test.command.CommandRunAssertion
import de.solidblocks.infra.test.files.DirectoryBuilder
import de.solidblocks.infra.test.files.file
import de.solidblocks.infra.test.files.tempDir
import de.solidblocks.infra.test.output.TimestampedOutputLine
import java.io.Closeable
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readBytes
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ScriptStep(val step: String, val assertion: ((CommandRunAssertion) -> Unit)?)

abstract class ScriptBuilder : Closeable {

  internal val includes: MutableList<Path> = mutableListOf()

  protected val steps: MutableList<ScriptStep> = mutableListOf()

  internal var sources = mutableListOf<Path>()

  internal var assertSteps = true

  internal var defaultWaitForOutput: Duration = 60.seconds

  internal val envs = mutableMapOf<String, String>()

  internal val resources = mutableListOf<Closeable>()

  internal var inheritEnv = true

  fun defaultWaitForOutput(defaultWaitForOutput: Duration) = apply {
    this.defaultWaitForOutput = defaultWaitForOutput
  }

  fun assertSteps(assertSteps: Boolean) = apply { this.assertSteps = assertSteps }

  fun includes(vararg includes: String) = apply {
    this.includes.addAll(includes.map { Path.of(it) })
  }

  fun includes(vararg includes: Path) = apply { this.includes.addAll(includes) }

  fun inheritEnv(inheritEnv: Boolean) = apply { this.inheritEnv = inheritEnv }

  fun sources(sources: DirectoryBuilder) = this.sources(sources.path)

  fun sources(sources: Path) = apply { this.sources.add(sources) }

  fun step(step: String, assertion: ((CommandRunAssertion) -> Unit)? = null) = apply {
    this.steps.add(ScriptStep(step, assertion))
  }

  protected fun buildScript(): Pair<Path, List<String>> {
    val tempDir = tempDir()
    resources.add(tempDir)

    this.sources.forEach { tempDir.copyFromDir(it) }

    val sourceMappings =
        this.includes.map { source ->
          source to
              tempDir.path.resolve(
                  source
                      .absolutePathString()
                      .removePrefix(File.separator)
                      .replace(File.separator, "_"),
              )
        }

    sourceMappings.forEach { sourceMapping ->
      tempDir
          .file(sourceMapping.second.absolutePathString())
          .content(sourceMapping.first.readBytes())
          .create()
    }

    val script = StringBuilder()

    script.appendLine("#!/usr/bin/env bash")
    script.appendLine("set -eu -o pipefail")

    sourceMappings.forEach { script.appendLine("source ${it.second.absolutePathString()}") }

    steps.forEachIndexed { index, step ->
      script.appendLine("echo \"starting step ${index}\"")
      script.appendLine(step.step)
      script.appendLine("echo \"finished step ${index}\"")
      script.appendLine("read")
    }

    val scriptFile = tempDir.file("script.sh").executable().content(script.toString()).create()

    return tempDir.path to listOf(scriptFile.absolutePathString())
  }

  fun env(env: Pair<String, String>) = apply { this.envs[env.first] = env.second }

  override fun close() {
    resources.forEach { it.close() }
  }

  abstract fun run(): CommandResult<TimestampedOutputLine>
}
