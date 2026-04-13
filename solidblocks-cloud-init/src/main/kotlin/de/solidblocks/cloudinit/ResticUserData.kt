package de.solidblocks.cloudinit

import de.solidblocks.shell.*
import de.solidblocks.shell.ResticLibrary.RESTIC_CREDENTIALS_PATH
import de.solidblocks.shell.systemd.Daily
import de.solidblocks.shell.systemd.Install
import de.solidblocks.shell.systemd.Restart
import de.solidblocks.shell.systemd.Service
import de.solidblocks.shell.systemd.ServiceType
import de.solidblocks.shell.systemd.SystemDConfig
import de.solidblocks.shell.systemd.SystemDService
import de.solidblocks.shell.systemd.SystemDTimer
import de.solidblocks.shell.systemd.Timer
import de.solidblocks.shell.systemd.Unit
import de.solidblocks.shell.systemd.installSystemDUnit

const val RESTIC_STATUS_COMMAND = "restic-status"

private fun String.systemDUnitName(serviceName: String) = "$serviceName-backup-${this.removePrefix("/").removeSuffix("/").replace(Regex("[^a-zA-Z0-9]"), "-").lowercase()}"

fun ShellScript.resticBackup(serviceName: String, backupConfig: BackupConfiguration, backupPath: String) {
    addLibSources(CurlLibrary)
    addLibSources(ResticLibrary)

    addInlineSource(PackageLibrary)
    addCommand(PackageLibrary.InstallPackage("jq"))
    addCommand(ResticLibrary.Install())

    when (backupConfig.target) {
        is LocalBackupTarget -> {
            val backupMount = "/storage/backup"
            val localRepository = "$backupMount/$serviceName"
            addCommand(StorageLibrary.Mount(backupConfig.target.backupDevice, backupMount))

            addCommand(ResticLibrary.WriteCredentials(backupConfig.password))
            addCommand(ResticLibrary.EnsureLocalRepo(localRepository))
            installResticStatusWrapper(localRepository, backupPath)
            installBackupUnitWithTrigger(localBackupSystemDUnit(serviceName, localRepository, backupPath))
            addCommand(ResticLibrary.Restore(localRepository))
        }

        is S3BackupTarget -> {
            val s3Repository = "s3:s3.eu-central-1.amazonaws.com/${backupConfig.target.bucket}/$serviceName"

            addCommand(ResticLibrary.WriteS3Credentials(backupConfig.password, backupConfig.target.accessKey, backupConfig.target.secretKey))
            addCommand(ResticLibrary.EnsureS3Repo(s3Repository))
            installResticStatusWrapper(s3Repository, backupPath)
            installBackupUnitWithTrigger(s3BackupSystemDUnit(serviceName, s3Repository, backupPath))
            addCommand(ResticLibrary.Restore(s3Repository))
        }
    }
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
    addCommand(SystemDLibrary.Enable(timer.fullUnitName()))
    addCommand(SystemDLibrary.Start(timer.fullUnitName()))
}

fun localBackupSystemDUnit(serviceName: String, localRepository: String, backupPath: String) = SystemDService(
    backupPath.systemDUnitName(serviceName),
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
        type = ServiceType.oneshot,
        restart = Restart.ON_FAILURE,
        environmentFiles = listOf(RESTIC_CREDENTIALS_PATH),
    ),
    Install(),
)

fun s3BackupSystemDUnit(serviceName: String, s3Repository: String, backupPath: String) = SystemDService(
    backupPath.systemDUnitName(serviceName),
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
        type = ServiceType.oneshot,
        restart = Restart.ON_FAILURE,
        environmentFiles = listOf(RESTIC_CREDENTIALS_PATH),
    ),
    Install(),
)

private fun ShellScript.installResticStatusWrapper(repository: String, backupPath: String) {
    val wrapper = """
    #!/bin/env bash
    
    export $(cat ${RESTIC_CREDENTIALS_PATH} | xargs)
    restic --repo $repository snapshots --json
    """.trimIndent()

    addCommand(
        WriteFile(
            wrapper.toByteArray(),
            "/usr/local/bin/${RESTIC_STATUS_COMMAND}",
            FilePermissions.R_XR_XR_X,
        ),
    )
}
