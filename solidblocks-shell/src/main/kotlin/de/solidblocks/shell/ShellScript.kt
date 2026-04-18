package de.solidblocks.shell

import java.io.StringWriter
import kotlin.io.encoding.Base64

data class ShellScript(var environmentVariables: Map<String, String> = mutableMapOf()) {

    val commands = ArrayList<ShellCommand>()
    val libSources = mutableMapOf<String, String>()

    companion object {
        const val LIB_SOURCES_PATH = "/usr/lib/blcks"
    }

    fun render(inlineLibSources: Boolean = true): String {
        val sw = StringWriter()
        sw.appendLine("#!/usr/bin/env bash")
        sw.appendLine("set -eu -o pipefail")

        if (inlineLibSources) {
            if (libSources.isNotEmpty()) {
                sw.appendLine("mkdir -p ${LIB_SOURCES_PATH}")
            }

            libSources.forEach {
                sw.appendLine(
                    "echo '${Base64.encode(it.value.toByteArray())}' | base64 -d > ${it.key}",
                )
            }
        }

        libSources.forEach {
            sw.appendLine("source ${it.key}")
        }

        commands.forEach { it.commands().forEach { shell -> sw.appendLine(shell) } }

        return sw.toString()
    }

    fun addCommand(command: ShellCommand) = commands.add(command)

    fun addLibrary(library: ShellLibrary) {
        libSources["${LIB_SOURCES_PATH}/${library.name()}.sh"] = library.source()
    }
}
