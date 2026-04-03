package de.solidblocks.restic

import de.solidblocks.shell.CurlLibrary
import de.solidblocks.shell.PackageLibrary
import de.solidblocks.shell.ResticLibrary
import de.solidblocks.shell.ResticLibrary.RESTIC_CREDENTIALS_PATH
import de.solidblocks.shell.ShellScript
import de.solidblocks.shell.SystemDLibrary
import de.solidblocks.systemd.Daily
import de.solidblocks.systemd.Install
import de.solidblocks.systemd.Restart
import de.solidblocks.systemd.Service
import de.solidblocks.systemd.SystemDConfig
import de.solidblocks.systemd.SystemDService
import de.solidblocks.systemd.SystemDTimer
import de.solidblocks.systemd.Timer
import de.solidblocks.systemd.Unit
import de.solidblocks.systemd.installSystemDUnit

private fun String.toBackupName1(): String =
    this.removePrefix("/").removeSuffix("/").replace("/", "-")

private fun String.s3SystemDUnitName() = "backup-${toBackupName1()}-s3"

private fun String.localSystemDUnitName() = "backup-${toBackupName1()}-local"

private fun ShellScript.resticCommon() {
  addLibSources(CurlLibrary)
  addLibSources(ResticLibrary)

  addInlineSource(PackageLibrary)
  addCommand(PackageLibrary.InstallPackage("jq"))
  addCommand(ResticLibrary.Install())
}

fun ShellScript.installBackupUnitWithTrigger(config: SystemDConfig) {
  installSystemDUnit(config)
  val timer =
      SystemDTimer(
          config.name,
          Unit("backup for '${config.name}'"),
          Timer(
              Daily(),
              config.fullUnitName(),
          ),
          Install(),
      )
  installSystemDUnit(timer)
  addCommand(SystemDLibrary.Start(timer.fullUnitName()))
}

fun localBackupSystemDUnit(localRepository: String, backupPath: String) =
    SystemDService(
        backupPath.localSystemDUnitName(),
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
        Install(),
    )

fun s3BackupSystemDUnit(s3Repository: String, backupPath: String) =
    SystemDService(
        backupPath.s3SystemDUnitName(),
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
        Install(),
    )

fun ShellScript.resticLocalBackup(
    localRepository: String,
    repositoryPassword: String,
    backupPath: String,
) {
  resticCommon()
  addCommand(ResticLibrary.WriteCredentials(repositoryPassword))
  addCommand(ResticLibrary.EnsureLocalRepo(localRepository))
  addCommand(ResticLibrary.Restore(localRepository))

  installBackupUnitWithTrigger(localBackupSystemDUnit(localRepository, backupPath))
}

fun ShellScript.resticS3Backup(
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
  installBackupUnitWithTrigger(s3BackupSystemDUnit(s3Repository, backupPath))
}

fun ShellScript.resticLocalAndS3Backup(
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

  installBackupUnitWithTrigger(localBackupSystemDUnit(localRepository, backupPath))
  installBackupUnitWithTrigger(s3BackupSystemDUnit(s3Repository, backupPath))
}
