package de.solidblocks.docker

import de.solidblocks.caddy.*
import de.solidblocks.cloudinit.ServiceUserData
import de.solidblocks.shell.*
import de.solidblocks.shell.MkDir
import de.solidblocks.shell.ShellScript
import de.solidblocks.systemd.Install
import de.solidblocks.systemd.Restart
import de.solidblocks.systemd.Service
import de.solidblocks.systemd.SystemDService
import de.solidblocks.systemd.Target
import de.solidblocks.systemd.Unit
import de.solidblocks.systemd.installSystemDUnit

class GenericDockerServiceUserData(
    val name: String,
    val linuxDevice: String,
    val rootDomain: String?,
    val dockerImage: String,
    val ports: Map<Int, Int>,
    val enableHttps: Boolean = false,
) : ServiceUserData {

  override fun render(): String {
    val storageMount = "/storage/data"
    val caddyStorageDir = "$storageMount/www"

    val caddyConfig =
        CaddyConfig(
            GlobalOptions(
                FileSystemStorage(caddyStorageDir),
                "info@${rootDomain ?: "localhost"}",
                if (enableHttps) {
                  null
                } else {
                  AutoHttps.off
                },
            ),
            ports.map { Site(":${it.key}", ReverseProxy("http://localhost:${it.value}")) },
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
    userData.addCommand(StorageLibrary.Mount(linuxDevice, storageMount))

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

    val dockerWorkingDirectory = "/usr/local/etc/containers/"
    val dockerComposeFile = "$dockerWorkingDirectory/docker-compose.yml"
    val environment = mapOf<String, String>()

    val dockerCompose =
        ComposeFile(
            services =
                mapOf(
                    name to
                        Service(
                            image = dockerImage,
                            ports =
                                ports.map {
                                  PortMapping(
                                      it.key,
                                      it.value,
                                  )
                                },
                        ),
                ),
        )
    userData.addCommand(MkDir(dockerWorkingDirectory))
    userData.addCommand(WriteFile(dockerCompose.toYaml().toByteArray(), dockerComposeFile))

    val dockerSystemDConfig =
        SystemDService(
            name,
            Unit(
                "'$name' docker compose service",
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
    userData.addCommand(SystemDLibrary.Restart(name))

    return userData.render()
  }
}
