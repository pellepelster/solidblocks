package de.solidblocks.cloudinit.postgresql

import de.solidblocks.cloudinit.BackupConfiguration
import de.solidblocks.cloudinit.LocalBackupTarget
import de.solidblocks.cloudinit.S3BackupTarget
import de.solidblocks.cloudinit.ServiceUserData
import de.solidblocks.cloudinit.docker.ComposeFile
import de.solidblocks.cloudinit.docker.Mount
import de.solidblocks.cloudinit.docker.MountType
import de.solidblocks.cloudinit.docker.PortMapping
import de.solidblocks.cloudinit.docker.toYaml
import de.solidblocks.shell.AptLibrary
import de.solidblocks.shell.CurlLibrary
import de.solidblocks.shell.DockerLibrary
import de.solidblocks.shell.FilePermissions
import de.solidblocks.shell.LogLibrary
import de.solidblocks.shell.MkDir
import de.solidblocks.shell.PackageLibrary
import de.solidblocks.shell.ShellScript
import de.solidblocks.shell.StorageLibrary
import de.solidblocks.shell.SystemDLibrary
import de.solidblocks.shell.UtilsLibrary
import de.solidblocks.shell.WriteFile
import de.solidblocks.systemd.Daily
import de.solidblocks.systemd.Install
import de.solidblocks.systemd.Restart
import de.solidblocks.systemd.Service
import de.solidblocks.systemd.ServiceType
import de.solidblocks.systemd.SystemDService
import de.solidblocks.systemd.SystemDTimer
import de.solidblocks.systemd.Target
import de.solidblocks.systemd.Timer
import de.solidblocks.systemd.Unit
import de.solidblocks.systemd.installSystemDUnit

class PostgresqlUserData(val instanceName: String, val superUserPassword: String, val storageDevice: String, val backupConfiguration: BackupConfiguration) : ServiceUserData {

    companion object {
        val BACKUP_STATUS_COMMAND = "pgbackrest-status"
    }

    override fun render(): String {
        val storageMount = "/storage/data"
        val backupMount = "/storage/backup"

        val userData = ShellScript()

        userData.addInlineSource(UtilsLibrary)

        userData.addInlineSource(AptLibrary)
        userData.addInlineSource(CurlLibrary)
        userData.addInlineSource(DockerLibrary)
        userData.addInlineSource(LogLibrary)

        userData.addInlineSource(PackageLibrary)
        userData.addCommand(PackageLibrary.UpdateRepositories())
        userData.addCommand(PackageLibrary.UpdateSystem())

        userData.addInlineSource(StorageLibrary)
        userData.addCommand(StorageLibrary.Mount(storageDevice, storageMount))

        if (backupConfiguration.target is LocalBackupTarget) {
            userData.addCommand(StorageLibrary.Mount(backupConfiguration.target.backupDevice, backupMount))
        }

        val backupEnvironment = when (backupConfiguration.target) {
            is LocalBackupTarget -> mapOf(
                "DB_BACKUP_LOCAL" to "1",
                "DB_BACKUP_ENCRYPTION_PASSPHRASE" to superUserPassword,
            )

            is S3BackupTarget -> mapOf(
                "DB_BACKUP_S3" to "1",
                "DB_BACKUP_S3_BUCKET" to backupConfiguration.target.bucket,
                "DB_BACKUP_S3_ACCESS_KEY" to backupConfiguration.target.accessKey,
                "DB_BACKUP_S3_SECRET_KEY" to backupConfiguration.target.secretKey,
                "DB_BACKUP_ENCRYPTION_PASSPHRASE" to superUserPassword,
            )
        }

        val backupMounts = when (backupConfiguration.target) {
            is LocalBackupTarget -> listOf(
                Mount(MountType.bind, backupMount, backupMount),
            )

            else -> emptyList()
        }

        userData.addCommand(DockerLibrary.InstallDebian())
        userData.addCommand(MkDir("/storage/data", "10000", "10000"))
        if (backupConfiguration.target is LocalBackupTarget) {
            userData.addCommand(MkDir(backupMount, "10000", "10000"))
        }

        val dockerWorkingDirectory = "/etc/docker/$instanceName"
        val dockerComposeFile = "$dockerWorkingDirectory/docker-compose.yml"
        val environment = mapOf<String, String>()

        val dockerCompose =
            ComposeFile(
                services =
                mapOf(
                    instanceName to
                        de.solidblocks.cloudinit.docker.Service(
                            image = "ghcr.io/pellepelster/solidblocks-rds-postgresql:17-v0.4.15",
                            environment =
                            mapOf(
                                "DB_INSTANCE_NAME" to instanceName,
                                "DB_ADMIN_PASSWORD" to superUserPassword,
                            ) + backupEnvironment,
                            volumes =
                            listOf(
                                Mount(MountType.bind, storageMount, "/storage/data"),
                            ) + backupMounts,
                            ports =
                            listOf(
                                PortMapping(
                                    5432,
                                    5432,
                                ),
                            ),
                        ),
                ),
            )
        userData.addCommand(MkDir(dockerWorkingDirectory))
        userData.addCommand(WriteFile(dockerCompose.toYaml().toByteArray(), dockerComposeFile))

        val dockerSystemDConfig =
            SystemDService(
                instanceName,
                Unit(
                    "PostgresSQL instance '$instanceName'",
                    after = listOf(Target.DOCKER_SERVICE),
                    requires = listOf(Target.DOCKER_SERVICE),
                ),
                Service(
                    listOf(
                        "/usr/bin/docker",
                        "compose",
                        "--file",
                        dockerComposeFile,
                        "up",
                        "--force-recreate",
                    ),
                    restart = Restart.ALWAYS,
                    environment = environment,
                    workingDirectory = dockerWorkingDirectory,
                    execDown =
                    listOf("/usr/bin/docker", "compose", "--file", dockerComposeFile, "down"),
                ),
                Install(),
            )

        userData.installSystemDUnit(dockerSystemDConfig)

        val backupFullSystemDConfig =
            SystemDService(
                "$instanceName-backup-full",
                Unit(
                    "full backup for PostgresSQL instance '$instanceName'",
                    after = emptyList(),
                    requires = emptyList(),
                ),
                Service(
                    listOf(
                        "/usr/bin/docker",
                        "compose",
                        "--file",
                        dockerComposeFile,
                        "exec",
                        "--no-tty",
                        instanceName,
                        "/rds/bin/backup-full.sh",
                    ),
                    type = ServiceType.oneshot,
                    workingDirectory = dockerWorkingDirectory,
                ),
                Install(),
            )

        userData.installSystemDUnit(backupFullSystemDConfig)

        val timer =
            SystemDTimer(
                backupFullSystemDConfig.name,
                Unit("full backup for PostgresSQL instance '$instanceName'"),
                Timer(
                    Daily(),
                    backupFullSystemDConfig.fullUnitName(),
                ),
                Install(),
            )
        userData.installSystemDUnit(timer)
        userData.addCommand(SystemDLibrary.Start(timer.fullUnitName()))
        userData.addCommand(SystemDLibrary.Restart(instanceName))

        val wrapper = """
    #!/bin/env bash
    docker compose -f $dockerComposeFile exec --no-tty $instanceName /rds/bin/backup-info.sh --output=json
        """.trimIndent()

        userData.addCommand(
            WriteFile(
                wrapper.toByteArray(),
                "/usr/local/bin/$BACKUP_STATUS_COMMAND",
                FilePermissions.R_XR_XR_X,
            ),
        )

        return userData.render()
    }
}
