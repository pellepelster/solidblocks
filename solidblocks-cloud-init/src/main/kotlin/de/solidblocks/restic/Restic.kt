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

private fun String.toBackupName1(): String =
    this.removePrefix("/").removeSuffix("/").replace("/", "-")

private fun String.s3SystemDUnitName() = "backup-${toBackupName1()}-s3"

private fun String.localSystemDUnitName() = "backup-${toBackupName1()}-s3"

private fun CloudInitUserData.resticCommon() {
  addLibSources("curl", CurlLibrary.source())
  addLibSources("restic", ResticLibrary.source())

  addSources(PackageLibrary.source())
  addCommand(PackageLibrary.InstallPackage("jq"))
  addCommand(ResticLibrary.Install())
}

fun localBackupSystemDUnit(localRepository: String, backupPath: String) =
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
            environmentFiles = listOf(RESTIC_CREDENTIALS_PATH),
        ),
        null,
    )

fun s3BackupSystemDUnit(s3Repository: String, backupPath: String) =
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
            environmentFiles = listOf(RESTIC_CREDENTIALS_PATH),
        ),
        null,
    )

fun CloudInitUserData.resticLocalBackup(
    localRepository: String,
    repositoryPassword: String,
    backupPath: String,
) {
  resticCommon()
  addCommand(ResticLibrary.WriteCredentials(repositoryPassword))
  addCommand(ResticLibrary.EnsureLocalRepo(localRepository))
  addCommand(ResticLibrary.Restore(localRepository))
  installSystemDUnit(
      backupPath.localSystemDUnitName(),
      localBackupSystemDUnit(localRepository, backupPath),
  )
}

fun CloudInitUserData.resticS3Backup(
    s3Repository: String,
    repositoryPassword: String,
    awsAccessKey: String,
    awsSecretKey: String,
    backupPath: String,
) {
  resticCommon()

  addCommand(ResticLibrary.WriteS3Credentials(repositoryPassword, awsAccessKey, awsSecretKey))
  addCommand(ResticLibrary.EnsureS3Repo(s3Repository))
  addCommand(ResticLibrary.Restore(s3Repository))
  installSystemDUnit(backupPath.s3SystemDUnitName(), s3BackupSystemDUnit(s3Repository, backupPath))
}

fun CloudInitUserData.resticLocalAndS3Backup(
    localRepository: String,
    s3Repository: String,
    repositoryPassword: String,
    awsAccessKey: String,
    awsSecretKey: String,
    backupPath: String,
) {
  resticCommon()

  addCommand(ResticLibrary.WriteS3Credentials(repositoryPassword, awsAccessKey, awsSecretKey))

  addCommand(ResticLibrary.EnsureLocalRepo(localRepository))
  addCommand(ResticLibrary.EnsureS3Repo(s3Repository))

  addCommand(ResticLibrary.Restore(localRepository))
  addCommand(ResticLibrary.Restore(s3Repository))

  installSystemDUnit(
      backupPath.localSystemDUnitName(),
      localBackupSystemDUnit(localRepository, backupPath),
  )
  installSystemDUnit(backupPath.s3SystemDUnitName(), s3BackupSystemDUnit(s3Repository, backupPath))
}
