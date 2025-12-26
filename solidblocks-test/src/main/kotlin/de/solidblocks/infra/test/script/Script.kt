package de.solidblocks.infra.test.script

import de.solidblocks.infra.test.command.CommandResult
import de.solidblocks.infra.test.command.CommandRunAssertion
import de.solidblocks.infra.test.files.DirectoryBuilder
import de.solidblocks.infra.test.files.file
import de.solidblocks.infra.test.files.tempDir
import de.solidblocks.infra.test.output.TimestampedOutputLine
import java.io.Closeable
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.absolutePathString
import kotlin.io.path.readBytes
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ScriptStep(val step: String, val assertion: ((CommandRunAssertion) -> Unit)?)

abstract class ScriptBuilder : Closeable {

  val workingDir = tempDir()

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

  fun String.hashedWithSha256() =
      MessageDigest.getInstance("SHA-256").digest(toByteArray()).toHexString()

  fun includes(vararg includes: URI) = apply {
    for (include in includes) {
      val hashedURI = include.toString().hashedWithSha256()
      val includedURIFile = workingDir.file(hashedURI).content(include.toURL().readBytes()).create()
      this.includes.add(includedURIFile)
    }
  }

  fun inheritEnv(inheritEnv: Boolean) = apply { this.inheritEnv = inheritEnv }

  fun sources(sources: DirectoryBuilder) = this.sources(sources.path)

  fun sources(sources: Path) = apply { this.sources.add(sources) }

  fun step(step: String, assertion: ((CommandRunAssertion) -> Unit)? = null) = apply {
    this.steps.add(ScriptStep(step, assertion))
  }

  protected fun buildScript(): Pair<Path, List<String>> {
    resources.add(workingDir)

    this.sources.forEach { workingDir.copyFromDir(it) }

    val sourceMappings =
        this.includes.map { source ->
          source to
              workingDir.path.resolve(
                  source
                      .absolutePathString()
                      .removePrefix(File.separator)
                      .replace(File.separator, "_"),
              )
        }

    sourceMappings.forEach { sourceMapping ->
      workingDir
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

    val scriptFile = workingDir.file("script.sh").executable().content(script.toString()).create()

    return workingDir.path to listOf(scriptFile.absolutePathString())
  }

  fun env(env: Pair<String, String>) = apply { this.envs[env.first] = env.second }

  override fun close() {
    resources.forEach { it.close() }
  }

  abstract fun run(): CommandResult<TimestampedOutputLine>
}
