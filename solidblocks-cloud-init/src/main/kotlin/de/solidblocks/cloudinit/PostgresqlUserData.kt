package de.solidblocks.cloudinit

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
import de.solidblocks.shell.docker.ComposeFile
import de.solidblocks.shell.docker.Mount
import de.solidblocks.shell.docker.MountType
import de.solidblocks.shell.docker.PortMapping
import de.solidblocks.shell.docker.Service
import de.solidblocks.shell.docker.toYaml
import de.solidblocks.shell.systemd.Daily
import de.solidblocks.shell.systemd.Install
import de.solidblocks.shell.systemd.Restart
import de.solidblocks.shell.systemd.ServiceType
import de.solidblocks.shell.systemd.SystemDService
import de.solidblocks.shell.systemd.SystemDTimer
import de.solidblocks.shell.systemd.Target
import de.solidblocks.shell.systemd.Timer
import de.solidblocks.shell.systemd.Unit
import de.solidblocks.shell.systemd.installSystemDUnit

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
                        Service(
                            image = "ghcr.io/pellepelster/solidblocks-rds-postgresql:17-v0.5.2",
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
                de.solidblocks.shell.systemd.Service(
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
                de.solidblocks.shell.systemd.Service(
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
                FilePermissions.Companion.R_XR_XR_X,
            ),
        )

        return userData.render()
    }
}
