package de.solidblocks.garagefs

import de.solidblocks.caddy.*
import de.solidblocks.cloudinit.ServiceUserData
import de.solidblocks.cloudinit.model.CloudInitUserData1
import de.solidblocks.cloudinit.model.File
import de.solidblocks.cloudinit.model.FilePermissions
import de.solidblocks.shell.*
import de.solidblocks.systemd.Service
import de.solidblocks.systemd.SystemdConfig
import de.solidblocks.systemd.Unit

data class GarageFsBucket(val name: String, val publicDomains: List<String>)

class GarageFsUserData(
    val linuxDevice: String,
    val baseDomainFqdn: String,
    val rpcSecret: String,
    val adminToken: String,
    val metricsToken: String,
    val buckets: List<GarageFsBucket>,
    val enableHttps: Boolean = false
) : ServiceUserData {
    override fun render(): String {

        val storageMount = "/storage/data"
        val caddyStorageDir = "${storageMount}/www"
        val caddyConfig =
            CaddyConfig(
                GlobalOptions(
                    FileSystemStorage(caddyStorageDir),
                    "info@${baseDomainFqdn}",
                    if (enableHttps) {
                        null
                    } else {
                        AutoHttps.off
                    }
                ),
                buckets.flatMap {
                    listOf(
                        Site("${it.name}.s3.${baseDomainFqdn}", ReverseProxy("http://localhost:3900")),
                        Site("${it.name}.s3-web.${baseDomainFqdn}", ReverseProxy("http://localhost:3902")),
                    ) + it.publicDomains.map {
                        Site(it, ReverseProxy("http://localhost:3902"))
                    }
                } + listOf(
                    Site("s3-admin.${baseDomainFqdn}", ReverseProxy("http://localhost:3903")),
                )
            )


        val userData = CloudInitUserData1()
        userData.addSources(UtilsLibrary.source())
        userData.addSources(AptLibrary.source())
        userData.addSources(CurlLibrary.source())
        userData.addSources(LogLibrary.source())
        userData.addSources(PackageLibrary.source())
        userData.addCommand(PackageLibrary.UpdateRepositories())
        userData.addCommand(PackageLibrary.UpdateSystem())

        userData.addSources(StorageLibrary.source())
        userData.addCommand(StorageLibrary.Mount(linuxDevice, storageMount))

        userData.addSources(CaddyLibrary.source())
        userData.addCommand(CaddyLibrary.Install())
        userData.addCommand(StorageLibrary.MkDir(caddyStorageDir, "caddy"))
        userData.addCommand(
            File(
                caddyConfig.render().toByteArray(), "/etc/caddy/Caddyfile", FilePermissions.RW_R__R__
            )
        )
        userData.addCommand(SystemDLibrary.SystemdRestartService("caddy"))

        val garageFsConfig = GarageFsConfig(
            storageMount,
            rpcSecret,
            adminToken,
            metricsToken,
            "s3.$baseDomainFqdn",
            "s3-web.$baseDomainFqdn",
        )

        val garageFsSystemdConfig =
            SystemdConfig(
                Unit("Garage Data Store"),
                Service(
                    listOf("/usr/local/bin/garage", "server"),
                    environment = mapOf("RUST_LOG" to "garage=info", "RUST_BACKTRACE" to "1"),
                    limitNOFILE = 42000,
                    stateDirectory = "garage"
                )
            )


        userData.addSources(GarageLibrary.source())
        userData.addCommand(GarageLibrary.Install())
        userData.addCommand(
            File(
                garageFsConfig.render().toByteArray(), "/etc/garage.toml", FilePermissions.RW_R__R__
            )
        )
        userData.addCommand(
            File(
                garageFsSystemdConfig.render().toByteArray(),
                "/etc/systemd/system/garage.service",
                FilePermissions.RW_R__R__
            )
        )
        userData.addCommand(StorageLibrary.MkDir(garageFsConfig.dataDir))
        userData.addCommand(StorageLibrary.MkDir(garageFsConfig.metaDataDir))
        userData.addCommand(SystemDLibrary.SystemdDaemonReload())

        userData.addCommand(SystemDLibrary.SystemdRestartService("garage"))
        userData.addCommand(GarageLibrary.ApplyLayout(4))

        return userData.render()
    }
}