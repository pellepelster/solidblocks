package de.solidblocks.cloudinit

import de.solidblocks.shell.AptLibrary
import de.solidblocks.shell.CaddyLibrary
import de.solidblocks.shell.CurlLibrary
import de.solidblocks.shell.DockerLibrary
import de.solidblocks.shell.FilePermissions
import de.solidblocks.shell.MkDir
import de.solidblocks.shell.ShellScript
import de.solidblocks.shell.StorageLibrary
import de.solidblocks.shell.SystemDLibrary
import de.solidblocks.shell.WriteFile
import de.solidblocks.shell.caddy.AutoHttps
import de.solidblocks.shell.caddy.CaddyConfig
import de.solidblocks.shell.caddy.FileSystemStorage
import de.solidblocks.shell.caddy.GlobalOptions
import de.solidblocks.shell.caddy.ReverseProxy
import de.solidblocks.shell.caddy.Site
import de.solidblocks.shell.docker.ComposeFile
import de.solidblocks.shell.docker.PortMapping
import de.solidblocks.shell.docker.Service
import de.solidblocks.shell.docker.toYaml
import de.solidblocks.shell.systemd.Install
import de.solidblocks.shell.systemd.Restart
import de.solidblocks.shell.systemd.SystemDService
import de.solidblocks.shell.systemd.Target
import de.solidblocks.shell.systemd.Unit
import de.solidblocks.shell.systemd.installSystemDUnit
import de.solidblocks.shell.toCloudInit

class GenericDockerServiceUserData(
    val serviceName: String,
    val dataDevice: String,
    val backupConfiguration: BackupConfiguration,
    val dockerImage: String,
    val ports: Map<Int, Int>,
    val serverFQDN: String?,
    val environmentVariables: Map<String, String> = emptyMap(),
) : ServiceUserData {
    override fun shellScript(): ShellScript {
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

        val shellScript = ShellScript()

        shellScript.addLibrary(AptLibrary)
        shellScript.addLibrary(CurlLibrary)
        shellScript.addCommand(AptLibrary.UpdateRepositories())
        shellScript.addCommand(AptLibrary.UpdateSystem())

        shellScript.addLibrary(DockerLibrary)
        shellScript.addLibrary(StorageLibrary)
        shellScript.addCommand(StorageLibrary.Mount(dataDevice, storageMount))
        shellScript.addCommand(DockerLibrary.InstallDebian())

        shellScript.addLibrary(CaddyLibrary)
        shellScript.addCommand(CaddyLibrary.Install())
        shellScript.addCommand(MkDir(caddyStorageDir, "caddy"))
        shellScript.addCommand(
            WriteFile(
                caddyConfig.render().toByteArray(),
                "/etc/caddy/Caddyfile",
                FilePermissions.Companion.RW_R__R__,
            ),
        )
        shellScript.addCommand(SystemDLibrary.Restart("caddy"))

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
        shellScript.addCommand(MkDir(dockerWorkingDirectory))
        shellScript.addCommand(WriteFile(dockerCompose.toYaml().toByteArray(), dockerComposeFile))

        val dockerSystemDConfig =
            SystemDService(
                serviceName,
                Unit(
                    "'$serviceName' docker compose service",
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

        shellScript.installSystemDUnit(dockerSystemDConfig)
        shellScript.addCommand(SystemDLibrary.Restart(serviceName))
        shellScript.resticBackup(serviceName, backupConfiguration, serviceDataDir)

        return shellScript
    }
}
