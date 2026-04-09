package de.solidblocks.postgresql

import de.solidblocks.cloudinit.ServiceUserData
import de.solidblocks.docker.*
import de.solidblocks.shell.*
import de.solidblocks.systemd.*
import de.solidblocks.systemd.Service
import de.solidblocks.systemd.Target
import de.solidblocks.systemd.Unit

class PostgresqlUserData(val storageDevice: String, val backupDevice: String, val instanceName: String, val superUserPassword: String) : ServiceUserData {

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
        userData.addCommand(StorageLibrary.Mount(backupDevice, backupMount))

        userData.addCommand(DockerLibrary.InstallDebian())
        userData.addCommand(MkDir("/storage/data", "10000", "10000"))
        userData.addCommand(MkDir("/storage/backup", "10000", "10000"))

        val dockerWorkingDirectory = "/etc/docker/$instanceName"
        val dockerComposeFile = "$dockerWorkingDirectory/docker-compose.yml"
        val environment = mapOf<String, String>()

        val dockerCompose =
            ComposeFile(
                services =
                mapOf(
                    instanceName to
                        de.solidblocks.docker.Service(
                            image = "ghcr.io/pellepelster/solidblocks-rds-postgresql:17-v0.4.15",
                            environment =
                            mapOf(
                                "DB_INSTANCE_NAME" to instanceName,
                                "DB_BACKUP_LOCAL" to "1",
                                "DB_ADMIN_PASSWORD" to superUserPassword,
                            ),
                            volumes =
                            listOf(
                                Mount(MountType.bind, storageMount, "/storage/data"),
                                Mount(MountType.bind, backupMount, "/storage/backup"),
                            ),
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
