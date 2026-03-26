package de.solidblocks.postgresql

import de.solidblocks.cloudinit.ServiceUserData
import de.solidblocks.cloudinit.model.CloudInitUserData
import de.solidblocks.cloudinit.model.WriteFile
import de.solidblocks.cloudinit.model.installSystemDUnit
import de.solidblocks.docker.ComposeFile
import de.solidblocks.docker.Mount
import de.solidblocks.docker.MountType
import de.solidblocks.docker.PortMapping
import de.solidblocks.docker.toYaml
import de.solidblocks.shell.AptLibrary
import de.solidblocks.shell.CurlLibrary
import de.solidblocks.shell.DockerLibrary
import de.solidblocks.shell.LogLibrary
import de.solidblocks.shell.PackageLibrary
import de.solidblocks.shell.StorageLibrary
import de.solidblocks.shell.SystemDLibrary
import de.solidblocks.shell.UtilsLibrary
import de.solidblocks.systemd.Service
import de.solidblocks.systemd.SystemDConfig
import de.solidblocks.systemd.Target
import de.solidblocks.systemd.Unit

class PostgresqlUserData(
    val storageDevice: String,
    val backupDevice: String,
    val instanceName: String,
) : ServiceUserData {

  override fun render(): String {
    val storageMount = "/storage/data"
    val backupMount = "/storage/backup"

    val userData = CloudInitUserData()

    userData.addSources(UtilsLibrary.source())

    userData.addSources(AptLibrary.source())
    userData.addSources(CurlLibrary.source())
    userData.addSources(DockerLibrary.source())
    userData.addSources(LogLibrary.source())

    userData.addSources(PackageLibrary.source())
    userData.addCommand(PackageLibrary.UpdateRepositories())
    userData.addCommand(PackageLibrary.UpdateSystem())

    userData.addSources(StorageLibrary.source())
    userData.addCommand(StorageLibrary.Mount(storageDevice, storageMount))
    userData.addCommand(StorageLibrary.Mount(backupDevice, backupMount))

    userData.addCommand(DockerLibrary.InstallDebian())
    userData.addCommand(StorageLibrary.MkDir("/storage/data", "10000", "10000"))
    userData.addCommand(StorageLibrary.MkDir("/storage/backup", "10000", "10000"))

    val dockerWorkingDirectory = "/usr/local/etc/containers/"
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
    userData.addCommand(StorageLibrary.MkDir(dockerWorkingDirectory))
    userData.addCommand(WriteFile(dockerCompose.toYaml().toByteArray(), dockerComposeFile))

    val dockerSystemDConfig =
        SystemDConfig(
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
                environment = environment,
                workingDirectory = dockerWorkingDirectory,
                execDown =
                    listOf("/usr/bin/docker", "compose", "--file", dockerComposeFile, "down"),
            ),
        )

    userData.installSystemDUnit(instanceName, dockerSystemDConfig)
    userData.addCommand(SystemDLibrary.SystemdRestartService(instanceName))

    return userData.render()
  }
}
