package de.solidblocks.infra.test

import de.solidblocks.infra.test.docker.docker
import kotlinx.coroutines.runBlocking
import local
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readBytes

class ScriptBuilder() {

    private val includes: MutableList<Path> = mutableListOf()

    private val steps: MutableList<ScriptStep> = mutableListOf()

    private var sources: Path? = null

    private var assertSteps = true

    fun assertSteps(assertSteps: Boolean) = apply {
        this.assertSteps = assertSteps
    }

    fun includes(vararg includes: String) = apply {
        this.includes.addAll(includes.map { Path.of(it) })
    }

    fun includes(vararg includes: Path) = apply {
        this.includes.addAll(includes)
    }

    fun sources(source: Path) = apply {
        this.sources = source
    }

    fun step(step: String, assertion: ((CommandRunAssertion) -> Unit)? = null) = apply {
        this.steps.add(ScriptStep(step, assertion))
    }

    fun runLocal(): CommandRunResult = runBlocking {
        val buildScript = buildScript()
        val command = local().command(buildScript.second)

        if (assertSteps) {
            steps.forEachIndexed() { index, step ->
                command.assert {
                    it.waitForOutput(".*finished step ${index}.*") {
                        "continue"
                    }
                }
            }
        }

        command.runResult()
    }

    fun runDocker(): CommandRunResult {
        val buildScript = buildScript()
        val command = docker().command(buildScript.second).sourceDir(buildScript.first)

        if (assertSteps) {
            steps.forEachIndexed() { index, step ->
                command.assert {
                    it.waitForOutput(".*finished step ${index}.*") {
                        "continue"
                    }
                }
            }
        }

        return command.runResult()
    }

    private fun buildScript(): Pair<Path, Path> {
        val tempDir = tempDir()

        if (this.sources != null) {
            tempDir.copyFromDir(this.sources!!)
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

        return tempDir.path to scriptFile.file
    }

}

class ScriptStep(val step: String, assertion: ((CommandRunAssertion) -> Unit)?) {
}

fun script() = ScriptBuilder()
