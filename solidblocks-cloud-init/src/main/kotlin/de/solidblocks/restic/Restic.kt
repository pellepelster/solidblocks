package de.solidblocks.restic

import de.solidblocks.cloudinit.CloudInitUserData
import de.solidblocks.cloudinit.installSystemDUnit
import de.solidblocks.shell.CurlLibrary
import de.solidblocks.shell.PackageLibrary
import de.solidblocks.shell.ResticLibrary
import de.solidblocks.shell.ResticLibrary.RESTIC_CREDENTIALS_PATH
import de.solidblocks.systemd.Restart
import de.solidblocks.systemd.Service
import de.solidblocks.systemd.SystemDConfig
import de.solidblocks.systemd.Unit

fun String.toBackupName(): String = this.removePrefix("/").removeSuffix("/").replace("/", "-")

fun CloudInitUserData.resticLocalBackup(
    repositoryPath: String,
    repositoryPassword: String,
    backupPath: String,
) {
  addLibSources("curl", CurlLibrary.source())
  addLibSources("restic", ResticLibrary.source())

  val systemdD =
      SystemDConfig(
          Unit("local backup for '$backupPath'", emptyList(), emptyList()),
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
  installSystemDUnit("backup-${backupPath.toBackupName()}-local", systemdD)
}

fun CloudInitUserData.resticS3Backup(
    s3Repository: String,
    repositoryPassword: String,
    awsAccessKey: String,
    awsSecretKey: String,
    backupPath: String,
) {
  addLibSources("curl", CurlLibrary.source())
  addLibSources("restic", ResticLibrary.source())

  val systemdD =
      SystemDConfig(
          Unit("s3 backup for '$backupPath'", emptyList(), emptyList()),
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
  installSystemDUnit("backup-${backupPath.toBackupName()}-s3", systemdD)
}

fun CloudInitUserData.resticLocalAndS3Backup(
    localRepository: String,
    s3Repository: String,
    repositoryPassword: String,
    awsAccessKey: String,
    awsSecretKey: String,
    backupPath: String,
) {
  addLibSources("curl", CurlLibrary.source())
  addLibSources("restic", ResticLibrary.source())

  val s3SystemdD =
      SystemDConfig(
          Unit("s3 backup for '$backupPath'", emptyList(), emptyList()),
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

  val localSystemdD =
      SystemDConfig(
          Unit("local backup for '$backupPath'", emptyList(), emptyList()),
          Service(
              listOf(
                  "restic",
                  "backup",
                  "--repo",
                  localRepository,
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

  addCommand(ResticLibrary.EnsureLocalRepo(localRepository))
  addCommand(ResticLibrary.EnsureS3Repo(s3Repository))

  addCommand(ResticLibrary.Restore(localRepository))
  addCommand(ResticLibrary.Restore(s3Repository))

  installSystemDUnit("backup-${backupPath.toBackupName()}-local", localSystemdD)
  installSystemDUnit("backup-${backupPath.toBackupName()}-s3", s3SystemdD)
}
