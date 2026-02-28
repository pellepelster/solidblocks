package de.solidblocks.docker

import de.solidblocks.caddy.AutoHttps
import de.solidblocks.caddy.CaddyConfig
import de.solidblocks.caddy.FileSystemStorage
import de.solidblocks.caddy.GlobalOptions
import de.solidblocks.caddy.ReverseProxy
import de.solidblocks.caddy.Site
import de.solidblocks.cloudinit.ServiceUserData
import de.solidblocks.cloudinit.model.CloudInitUserData1
import de.solidblocks.cloudinit.model.FilePermissions
import de.solidblocks.cloudinit.model.WriteFile
import de.solidblocks.shell.*
import de.solidblocks.systemd.Service
import de.solidblocks.systemd.SystemdConfig
import de.solidblocks.systemd.Target
import de.solidblocks.systemd.Unit

class DockerUserData(
    val linuxDevice: String,
    val rootDomain: String,
    val dockerImage: String,
    val dockerHttpPort: Int,
    val enableHttps: Boolean = false,
) : ServiceUserData {

  override fun render(): String {
    val storageMount = "/storage/data"
    val caddyStorageDir = "$storageMount/www"

    val dockerHttpPortHost = 8080

    val caddyConfig =
        CaddyConfig(
            GlobalOptions(
                FileSystemStorage(caddyStorageDir),
                "info@$rootDomain",
                if (enableHttps) {
                  null
                } else {
                  AutoHttps.off
                },
            ),
            listOf(
                Site(":80", ReverseProxy("http://localhost:$dockerHttpPortHost")),
            ),
        )

    val userData = CloudInitUserData1()
    userData.addSources(UtilsLibrary.source())
    userData.addSources(AptLibrary.source())
    userData.addSources(CurlLibrary.source())
    userData.addSources(DockerLibrary.source())
    userData.addSources(LogLibrary.source())
    userData.addSources(PackageLibrary.source())
    userData.addCommand(PackageLibrary.UpdateRepositories())
    userData.addCommand(PackageLibrary.UpdateSystem())

    userData.addSources(StorageLibrary.source())
    userData.addCommand(StorageLibrary.Mount(linuxDevice, storageMount))

    userData.addCommand(DockerLibrary.InstallDebian())

    userData.addSources(CaddyLibrary.source())
    userData.addCommand(CaddyLibrary.Install())
    userData.addCommand(StorageLibrary.MkDir(caddyStorageDir, "caddy"))
    userData.addCommand(
        WriteFile(
            caddyConfig.render().toByteArray(),
            "/etc/caddy/Caddyfile",
            FilePermissions.RW_R__R__,
        ),
    )
    userData.addCommand(SystemDLibrary.SystemdRestartService("caddy"))

    val dockerWorkingDirectory = "/usr/local/etc/containers/"
    val dockerComposeFile = "$dockerWorkingDirectory/docker-compose.yml"
    val environment = mapOf<String, String>()

    val dockerCompose =
        ComposeFile(
            services =
                mapOf(
                    "nginx" to
                        Service(
                            image = dockerImage,
                            ports =
                                listOf(
                                    PortMapping(
                                        dockerHttpPort,
                                        dockerHttpPortHost,
                                    ),
                                ),
                        ),
                ),
        )
    userData.addCommand(StorageLibrary.MkDir(dockerWorkingDirectory))
    userData.addCommand(WriteFile(dockerCompose.toYaml().toByteArray(), dockerComposeFile))

    val dockerSystemdConfig =
        SystemdConfig(
            Unit(
                "Docker compose service",
                after = listOf(Target.DOCKER_SERVICE),
                requires = listOf(Target.DOCKER_SERVICE),
            ),
            Service(
                listOf(
                    "/usr/bin/docker-compose",
                    "--file",
                    dockerComposeFile,
                    "up",
                    "--force-recreate",
                ),
                environment = environment,
                workingDirectory = dockerWorkingDirectory,
                execDown = listOf("/usr/bin/docker-compose", "--file", dockerComposeFile, "down"),
            ),
        )

    userData.addCommand(
        WriteFile(
            dockerSystemdConfig.render().toByteArray(),
            "/etc/systemd/system/docker.service",
            FilePermissions.RW_R__R__,
        ),
    )
    userData.addCommand(SystemDLibrary.SystemdDaemonReload())
    userData.addCommand(SystemDLibrary.SystemdRestartService("docker"))

    return userData.render()
  }
}
