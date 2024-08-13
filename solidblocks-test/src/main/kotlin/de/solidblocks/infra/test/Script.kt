package de.solidblocks.infra.test

import de.solidblocks.infra.test.output.OutputMatcher
import local
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readBytes
import kotlin.time.Duration.Companion.seconds

class ScriptBuilder() {

    internal val sources: MutableList<Path> = mutableListOf()

    internal val steps: MutableList<ScriptStepBuilder> = mutableListOf()

    internal var sourceDir: Path? = null

    fun includes(vararg sources: String) = apply {
        this.sources.addAll(sources.map { Path.of(it) })
    }

    fun includes(vararg sources: Path) = apply {
        this.sources.addAll(sources)
    }

    fun sources(sourceDir: Path) = apply {
        this.sourceDir = sourceDir
    }

    fun step(step: String, callback: ((ScriptStepBuilder) -> Unit)? = null) = apply {
        val b = ScriptStepBuilder(step)
        callback?.invoke(b)
        this.steps.add(b)
    }

}

class ScriptStepBuilder(val step: String) {

    val outputMatchers = mutableListOf<OutputMatcher>()

    fun waitForOutput(regex: String) = apply {
        outputMatchers.add(OutputMatcher(regex.toRegex(), 5.seconds, null))
        this
    }
}

fun script() = ScriptBuilder()

fun ScriptBuilder.runLocal(): CommandRunResult {

    tempDir().use {
        if (this.sourceDir != null) {
            it.copyFromDir(this.sourceDir!!)
        }

        val sourceMappings = this.sources.map { source ->
            source to it.path.resolve(
                source.absolutePathString()
                    .removePrefix(File.separator)
                    .replace(File.separator, "_")
            )
        }

        sourceMappings.forEach { sourceMapping ->
            it.createFile(sourceMapping.second.absolutePathString())
                .content(sourceMapping.first.readBytes())
                .create()
        }

        val script = """
#!/usr/bin/env bash

set -eu -o pipefail

${sourceMappings.joinToString("\n") { "source ${it.second.absolutePathString()}" }}

${steps.joinToString("\n") { it.step }}

""".trimIndent()

        val scriptFile = it.createFile("script.sh").executable().content(script).create()
        return local().command(scriptFile.file).waitForOutputs(steps.flatMap { it.outputMatchers }).runResult()
    }
}