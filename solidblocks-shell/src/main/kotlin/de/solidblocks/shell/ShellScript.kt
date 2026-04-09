package de.solidblocks.shell

import java.io.StringWriter
import kotlin.io.encoding.Base64

data class ShellScript(var environmentVariables: Map<String, String> = mutableMapOf()) {
    val commands = ArrayList<LibraryCommand>()
    val inlineSources = mutableSetOf<String>()
    val libSources = mutableMapOf<String, String>()

    companion object {
        const val LIB_SOURCES_PATH = "/usr/lib/blcks"
    }

    fun render(): String {
        val sw = StringWriter()
        sw.appendLine(
            """
        #!/usr/bin/env bash

        set -eu -o pipefail
        """
                .trimIndent(),
        )

        inlineSources.forEach { sw.appendLine(it) }

        if (libSources.isNotEmpty()) {
            sw.appendLine("mkdir -p ${LIB_SOURCES_PATH}")
        }

        libSources.forEach {
            sw.appendLine(
                "echo '${Base64.encode(it.value.toByteArray())}' | base64 -d > /usr/lib/blcks/${it.key}.sh",
            )
            sw.appendLine("source ${LIB_SOURCES_PATH}/${it.key}.sh")
        }

        commands.forEach { it.commands().forEach { shell -> sw.appendLine(shell) } }

        return sw.toString()
    }

    fun addCommand(command: LibraryCommand) = commands.add(command)

    fun addInlineSource(library: ShellLibrary) {
        inlineSources.add(library.source())
    }

    fun addLibSources(library: ShellLibrary) {
        libSources[library.name()] = library.source()
    }
}
