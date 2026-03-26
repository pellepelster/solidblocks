package de.solidblocks.cloudinit.model

import de.solidblocks.shell.LibraryCommand
import de.solidblocks.shell.ResticLibrary
import de.solidblocks.shell.SystemDLibrary
import de.solidblocks.systemd.Restart
import de.solidblocks.systemd.Service
import de.solidblocks.systemd.SystemDConfig
import de.solidblocks.systemd.Unit
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

    sw.appendLine("mkdir -p /usr/lib/blcks")
    libSources.forEach {
      sw.appendLine(
          "echo '${Base64.encode(it.value.toByteArray())}' | base64 -d > /usr/lib/blcks/${it.key}.sh",
      )
      sw.appendLine("source /usr/lib/blcks/${it.key}.sh")
    }

    commands.forEach { it.toShell().forEach { shell -> sw.appendLine(shell) } }

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
          FilePermissions.RW_R__R__,
      ),
  )
  addCommand(SystemDLibrary.SystemdDaemonReload())
}

fun CloudInitUserData.installRestic(
    repositoryName: String,
    repositoryPath: String,
    repositoryPassword: String,
    backupPath: String,
) {
  addLibSources("restic", ResticLibrary.source())
  val systemdD =
      SystemDConfig(
          Unit("backup for '$repositoryName'", emptyList(), emptyList()),
          Service(
              listOf(
                  "restic",
                  "backup",
                  "--repo",
                  repositoryPath,
                  "--verbose",
                  "backup",
                  backupPath,
              ),
              restart = Restart.ON_FAILURE,
              environment = mapOf("RESTIC_PASSWORD" to repositoryPassword),
          ),
          null,
      )

  addCommand(ResticLibrary.Install())
  addCommand(ResticLibrary.EnsureRepo(repositoryPath, repositoryPassword))
  addCommand(ResticLibrary.Restore(repositoryPath, repositoryPassword))
  installSystemDUnit("backup-$repositoryName", systemdD)
}
