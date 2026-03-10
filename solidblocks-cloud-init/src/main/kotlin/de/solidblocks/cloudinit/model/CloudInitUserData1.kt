package de.solidblocks.cloudinit.model

import de.solidblocks.shell.LibraryCommand
import java.io.StringWriter

data class CloudInitUserData1(var environmentVariables: Map<String, String> = mutableMapOf()) {
  val commands = ArrayList<LibraryCommand>()
  val sources = mutableListOf<String>()

  fun render(): String {
    val sw = StringWriter()
    sw.appendLine(
        """
        #!/usr/bin/env bash

        set -eu -o pipefail
        """
            .trimIndent(),
    )

    sources.forEach { sw.appendLine(it) }

    commands.forEach { it.toShell().forEach { shell -> sw.appendLine(shell) } }

    return sw.toString()
  }

  fun addCommand(command: LibraryCommand) = commands.add(command)

  fun addSources(source: String) {
    sources.add(source)
  }
}
