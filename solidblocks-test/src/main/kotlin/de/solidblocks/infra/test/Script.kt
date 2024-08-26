package de.solidblocks.infra.test

import de.solidblocks.infra.test.command.CommandRunAssertion
import de.solidblocks.infra.test.command.CommandRunResult
import de.solidblocks.infra.test.docker.docker
import kotlinx.coroutines.runBlocking
import local
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readBytes
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ScriptBuilder {

    private val includes: MutableList<Path> = mutableListOf()

    private val steps: MutableList<ScriptStep> = mutableListOf()

    private var sources = mutableListOf<Path>()

    private var assertSteps = true

    private var defaultWaitForOutput: Duration = 60.seconds

    private val envs = mutableMapOf<String, String>()

    fun defaultWaitForOutput(defaultWaitForOutput: Duration) = apply {
        this.defaultWaitForOutput = defaultWaitForOutput
    }

    fun assertSteps(assertSteps: Boolean) = apply {
        this.assertSteps = assertSteps
    }

    fun includes(vararg includes: String) = apply {
        this.includes.addAll(includes.map { Path.of(it) })
    }

    fun includes(vararg includes: Path) = apply {
        this.includes.addAll(includes)
    }

    fun sources(sources: DirectoryBuilder) = this.sources(sources.path)

    fun sources(sources: Path) = apply {
        this.sources.add(sources)
    }

    fun step(step: String, assertion: ((CommandRunAssertion) -> Unit)? = null) = apply {
        this.steps.add(ScriptStep(step, assertion))
    }

    fun runLocal(): CommandRunResult = runBlocking {
        val buildScript = buildScript()
        val command = local().command(*buildScript.second.toTypedArray())
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

    fun runDocker(): CommandRunResult {
        val buildScript = buildScript()
        val command = docker().command(*buildScript.second.toTypedArray())
            .sourceDir(buildScript.first)
            .workingDir(buildScript.first)
            .env(envs)

        if (assertSteps) {
            steps.forEachIndexed() { index, step ->
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

        return command.runResult()
    }

    private fun buildScript(): Pair<Path, List<String>> {
        val tempDir = tempDir()

        this.sources.forEach {
            tempDir.copyFromDir(it)
        }

        val sourceMappings = this.includes.map { source ->
            source to tempDir.path.resolve(
                source.absolutePathString()
                    .removePrefix(File.separator)
                    .replace(File.separator, "_")
            )
        }

        sourceMappings.forEach { sourceMapping ->
            tempDir.createFile(sourceMapping.second.absolutePathString())
                .content(sourceMapping.first.readBytes())
                .create()
        }

        val script = StringBuilder()

        script.appendLine("#!/usr/bin/env bash")
        script.appendLine("set -eu -o pipefail")

        sourceMappings.forEach {
            script.appendLine("source ${it.second.absolutePathString()}")
        }

        steps.forEachIndexed { index, step ->
            script.appendLine("echo \"starting step ${index}\"")
            script.appendLine(step.step)
            script.appendLine("echo \"finished step ${index}\"")
            script.appendLine("read")
        }

        val scriptFile = tempDir.createFile("script.sh").executable().content(script.toString()).create()

        return tempDir.path to listOf(scriptFile.file.absolutePathString())
    }

    fun env(env: Pair<String, String>) = apply {
        this.envs[env.first] = env.second
    }

}

class ScriptStep(val step: String, val assertion: ((CommandRunAssertion) -> Unit)?)

fun script() = ScriptBuilder()
