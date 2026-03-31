package de.solidblocks.cloudinit

import de.solidblocks.shell.*
import de.solidblocks.systemd.SystemDConfig
import java.io.StringWriter
import kotlin.io.encoding.Base64

data class CloudInitUserData(var environmentVariables: Map<String, String> = mutableMapOf()) {
  val commands = ArrayList<LibraryCommand>()
  val sources = mutableSetOf<String>()
  val libSources = mutableMapOf<String, String>()

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

    if (libSources.isNotEmpty()) {
      sw.appendLine("mkdir -p /usr/lib/blcks")
    }

    libSources.forEach {
      sw.appendLine(
          "echo '${Base64.encode(it.value.toByteArray())}' | base64 -d > /usr/lib/blcks/${it.key}.sh",
      )
      sw.appendLine("source /usr/lib/blcks/${it.key}.sh")
    }

    commands.forEach { it.commands().forEach { shell -> sw.appendLine(shell) } }

    return sw.toString()
  }

  fun addCommand(command: LibraryCommand) = commands.add(command)

  fun addSources(source: String) {
    sources.add(source)
  }

  fun addLibSources(name: String, source: String) {
    libSources[name] = source
  }
}

fun CloudInitUserData.installSystemDUnit(name: String, config: SystemDConfig) {
  addCommand(
      WriteFile(
          config.render().toByteArray(),
          "/etc/systemd/system/$name.service",
          FilePermissions.Companion.RW_R__R__,
      ),
  )
  addCommand(SystemDLibrary.SystemdDaemonReload())
}
