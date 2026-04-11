package de.solidblocks.cloudinit.docker

import de.solidblocks.cloudinit.BackupConfiguration
import de.solidblocks.cloudinit.LocalBackupTarget
import de.solidblocks.cloudinit.ServiceUserData
import de.solidblocks.cloudinit.caddy.AutoHttps
import de.solidblocks.cloudinit.caddy.CaddyConfig
import de.solidblocks.cloudinit.caddy.FileSystemStorage
import de.solidblocks.cloudinit.caddy.GlobalOptions
import de.solidblocks.cloudinit.caddy.ReverseProxy
import de.solidblocks.cloudinit.caddy.Site
import de.solidblocks.restic.resticBackup
import de.solidblocks.shell.AptLibrary
import de.solidblocks.shell.CaddyLibrary
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
import de.solidblocks.systemd.Install
import de.solidblocks.systemd.Restart
import de.solidblocks.systemd.Service
import de.solidblocks.systemd.SystemDService
import de.solidblocks.systemd.Target
import de.solidblocks.systemd.Unit
import de.solidblocks.systemd.installSystemDUnit

class GenericDockerServiceUserData(
    val serviceName: String,
    val dataDevice: String,
    val backupConfiguration: BackupConfiguration,
    val dockerImage: String,
    val ports: Map<Int, Int>,
    val serverFQDN: String?,
    val environmentVariables: Map<String, String> = emptyMap(),
) : ServiceUserData {
    override fun render(): String {
        val storageMount = "/storage/data"
        val serviceDataDir = "$storageMount/$serviceName"
        val caddyStorageDir = "$serviceDataDir/www"

        val caddyConfig =
            CaddyConfig(
                GlobalOptions(
                    FileSystemStorage(caddyStorageDir),
                    serverFQDN?.let { "info@$it" },
                    if (serverFQDN != null) {
                        null
                    } else {
                        AutoHttps.off
                    },
                ),
                if (serverFQDN == null) {
                    ports.map {
                        Site(":${it.key}", ReverseProxy("http://localhost:${it.value + 1024}"))
                    }
                } else {
                    ports.map {
                        Site(serverFQDN, ReverseProxy("http://localhost:${it.value + 1024}"))
                    }
                },
            )

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
        userData.addCommand(StorageLibrary.Mount(dataDevice, storageMount))

        userData.addCommand(DockerLibrary.InstallDebian())

        userData.addInlineSource(CaddyLibrary)
        userData.addCommand(CaddyLibrary.Install())
        userData.addCommand(MkDir(caddyStorageDir, "caddy"))
        userData.addCommand(
            WriteFile(
                caddyConfig.render().toByteArray(),
                "/etc/caddy/Caddyfile",
                FilePermissions.RW_R__R__,
            ),
        )
        userData.addCommand(SystemDLibrary.Restart("caddy"))

        val dockerWorkingDirectory = "/usr/local/etc/containers"
        val dockerComposeFile = "$dockerWorkingDirectory/docker-compose.yml"
        val environment = mapOf<String, String>()

        val dockerCompose =
            ComposeFile(
                services =
                mapOf(
                    serviceName to
                        Service(
                            image = dockerImage,
                            ports =
                            ports.map {
                                PortMapping(
                                    it.value,
                                    it.value + 1024,
                                )
                            },
                            environment = environmentVariables,
                        ),
                ),
            )
        userData.addCommand(MkDir(dockerWorkingDirectory))
        userData.addCommand(WriteFile(dockerCompose.toYaml().toByteArray(), dockerComposeFile))

        val dockerSystemDConfig =
            SystemDService(
                serviceName,
                Unit(
                    "'$serviceName' docker compose service",
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
        userData.addCommand(SystemDLibrary.Restart(serviceName))
        userData.resticBackup(serviceName, backupConfiguration, serviceDataDir)

        return userData.render()
    }
}
