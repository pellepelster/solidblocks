package de.solidblocks.cloudinit

import de.solidblocks.shell.*
import de.solidblocks.shell.ResticLibrary.RESTIC_CREDENTIALS_PATH
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

fun CloudInitUserData.resticLocalBackup(
    name: String,
    repositoryPath: String,
    repositoryPassword: String,
    backupPath: String,
) {
  addLibSources("curl", CurlLibrary.source())
  addLibSources("restic", ResticLibrary.source())

  val systemdD =
      SystemDConfig(
          Unit("backup for '$name'", emptyList(), emptyList()),
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

  addSources(PackageLibrary.source())
  addCommand(PackageLibrary.InstallPackage("jq"))
  addCommand(ResticLibrary.Install())
  addCommand(ResticLibrary.WriteCredentials(repositoryPassword))
  addCommand(ResticLibrary.EnsureLocalRepo(repositoryPath))
  addCommand(ResticLibrary.Restore(repositoryPath))
  installSystemDUnit("backup-$name-local", systemdD)
}

fun CloudInitUserData.resticS3Backup(
    name: String,
    repositoryPassword: String,
    repositoryPath: String,
    bucket: String,
    awsRegion: String,
    awsAccessKey: String,
    awsSecretKey: String,
    backupPath: String,
) {
  addLibSources("curl", CurlLibrary.source())
  addLibSources("restic", ResticLibrary.source())

  val s3Repository = "s3:s3.$awsRegion.amazonaws.com/$bucket/$repositoryPath"

  val systemdD =
      SystemDConfig(
          Unit("backup for '$name'", emptyList(), emptyList()),
          Service(
              listOf(
                  "restic",
                  "backup",
                  "--repo",
                  s3Repository,
                  "--verbose",
                  "backup",
                  backupPath,
              ),
              restart = Restart.ON_FAILURE,
              environment = mapOf("RESTIC_PASSWORD" to repositoryPassword),
              environmentFiles = listOf(RESTIC_CREDENTIALS_PATH),
          ),
          null,
      )

  addSources(PackageLibrary.source())
  addCommand(PackageLibrary.InstallPackage("jq"))
  addCommand(ResticLibrary.Install())
  addCommand(ResticLibrary.WriteS3Credentials(repositoryPassword, awsAccessKey, awsSecretKey))
  addCommand(ResticLibrary.EnsureS3Repo(s3Repository))
  addCommand(ResticLibrary.Restore(s3Repository))
  installSystemDUnit("backup-$name-s3", systemdD)
}
