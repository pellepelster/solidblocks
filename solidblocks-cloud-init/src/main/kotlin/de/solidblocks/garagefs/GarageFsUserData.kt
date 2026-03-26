package de.solidblocks.garagefs

import de.solidblocks.caddy.AutoHttps
import de.solidblocks.caddy.CaddyConfig
import de.solidblocks.caddy.FileSystemStorage
import de.solidblocks.caddy.GlobalOptions
import de.solidblocks.caddy.ReverseProxy
import de.solidblocks.caddy.Site
import de.solidblocks.cloudinit.ServiceUserData
import de.solidblocks.cloudinit.model.CloudInitUserData
import de.solidblocks.cloudinit.model.FilePermissions
import de.solidblocks.cloudinit.model.WriteFile
import de.solidblocks.cloudinit.model.installSystemDUnit
import de.solidblocks.shell.AptLibrary
import de.solidblocks.shell.CaddyLibrary
import de.solidblocks.shell.CurlLibrary
import de.solidblocks.shell.GarageLibrary
import de.solidblocks.shell.LogLibrary
import de.solidblocks.shell.PackageLibrary
import de.solidblocks.shell.StorageLibrary
import de.solidblocks.shell.SystemDLibrary
import de.solidblocks.shell.UtilsLibrary
import de.solidblocks.systemd.Service
import de.solidblocks.systemd.SystemDConfig
import de.solidblocks.systemd.Unit

data class GarageFsBucket(val name: String, val publicDomains: Set<String>)

class GarageFsUserData(
    val linuxDeviceData: String,
    val serviceRootDomain: String,
    val rpcSecret: String,
    val adminToken: String,
    val metricsToken: String,
    val buckets: List<GarageFsBucket>,
    val enableHttps: Boolean = false,
) : ServiceUserData {

  companion object {
    fun s3Host(serviceRootDomain: String) = "s3.$serviceRootDomain"

    fun s3AdminHost(serviceRootDomain: String) = "s3-admin.$serviceRootDomain"
  }

  override fun render(): String {
    val storageMount = "/storage/data"
    val caddyStorageDir = "$storageMount/www"

    val caddyConfig =
        CaddyConfig(
            GlobalOptions(
                FileSystemStorage(caddyStorageDir),
                "info@$serviceRootDomain",
                if (enableHttps) {
                  null
                } else {
                  AutoHttps.off
                },
            ),
            buckets.flatMap {
              listOf(
                  Site("${it.name}.s3.$serviceRootDomain", ReverseProxy("http://localhost:3900")),
                  Site(
                      "${it.name}.s3-web.$serviceRootDomain",
                      ReverseProxy("http://localhost:3902"),
                  ),
              ) + it.publicDomains.map { Site(it, ReverseProxy("http://localhost:3902")) }
            } +
                listOf(
                    Site(s3AdminHost(serviceRootDomain), ReverseProxy("http://localhost:3903")),
                    Site(s3Host(serviceRootDomain), ReverseProxy("http://localhost:3900")),
                ),
        )

    val userData = CloudInitUserData()
    userData.addSources(UtilsLibrary.source())
    userData.addSources(AptLibrary.source())
    userData.addSources(CurlLibrary.source())
    userData.addSources(LogLibrary.source())
    userData.addSources(PackageLibrary.source())
    userData.addCommand(PackageLibrary.UpdateRepositories())
    userData.addCommand(PackageLibrary.UpdateSystem())

    userData.addSources(StorageLibrary.source())
    userData.addCommand(StorageLibrary.Mount(linuxDeviceData, storageMount))

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

    val garageFsConfig =
        GarageFsConfig(
            storageMount,
            rpcSecret,
            adminToken,
            metricsToken,
            "s3.$serviceRootDomain",
            "s3-web.$serviceRootDomain",
        )

    val garageFsSystemDConfig =
        SystemDConfig(
            Unit("Garage Data Store"),
            Service(
                listOf("/usr/local/bin/garage", "server"),
                environment = mapOf("RUST_LOG" to "garage=info", "RUST_BACKTRACE" to "1"),
                limitNOFILE = 42000,
                stateDirectory = "garage",
            ),
        )

    userData.addSources(GarageLibrary.source())
    userData.addCommand(GarageLibrary.Install())
    userData.addCommand(
        WriteFile(
            garageFsConfig.render().toByteArray(),
            "/etc/garage.toml",
            FilePermissions.RW_R__R__,
        ),
    )
    userData.addCommand(StorageLibrary.MkDir(garageFsConfig.dataDir))
    userData.addCommand(StorageLibrary.MkDir(garageFsConfig.metaDataDir))

    userData.installSystemDUnit("garage", garageFsSystemDConfig)
    userData.addCommand(SystemDLibrary.SystemdRestartService("garage"))

    return userData.render()
  }
}
