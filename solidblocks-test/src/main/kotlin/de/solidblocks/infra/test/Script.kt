package de.solidblocks.infra.test

import local
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readBytes

class ScriptBuilder() {

    internal val sources: MutableList<Path> = mutableListOf()

    internal val steps: MutableList<String> = mutableListOf()

    fun sources(vararg sources: String) = apply {
        this.sources.addAll(sources.map { Path.of(it) })
    }

    fun sources(vararg sources: Path) = apply {
        this.sources.addAll(sources)
    }

    fun step(test: String, step: (StepBuilder) -> Unit) = apply {
        this.steps.add(test)
    }

}

class StepBuilder() {
}

fun script() = ScriptBuilder()

fun ScriptBuilder.runLocal() {
    tempDir().use {

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

${steps.joinToString("\n") { it }}

""".trimIndent()

        val scriptFile = it.createFile("script.sh").executable().content(script).create()
        val result = local().command(scriptFile.file).runResult()
    }
}