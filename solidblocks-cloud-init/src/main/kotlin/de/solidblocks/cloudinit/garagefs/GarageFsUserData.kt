package de.solidblocks.cloudinit.garagefs

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
import de.solidblocks.shell.FilePermissions
import de.solidblocks.shell.GarageLibrary
import de.solidblocks.shell.LogLibrary
import de.solidblocks.shell.MkDir
import de.solidblocks.shell.PackageLibrary
import de.solidblocks.shell.ShellScript
import de.solidblocks.shell.StorageLibrary
import de.solidblocks.shell.SystemDLibrary
import de.solidblocks.shell.UtilsLibrary
import de.solidblocks.shell.WriteFile
import de.solidblocks.systemd.Install
import de.solidblocks.systemd.Service
import de.solidblocks.systemd.SystemDService
import de.solidblocks.systemd.Unit
import de.solidblocks.systemd.installSystemDUnit

data class GarageFsBucket(val name: String, val publicDomains: Set<String>)

class GarageFsUserData(
    val serviceName: String,
    val dataDevice: String,
    val backupConfiguration: BackupConfiguration,
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
        val backupMount = "/storage/backup"
        val serviceDataDir = "$storageMount/$serviceName"
        val caddyDataDir = "$serviceDataDir/www"
        val garageFsDataDir = "$serviceDataDir/garage"

        val caddyConfig =
            CaddyConfig(
                GlobalOptions(
                    FileSystemStorage(caddyDataDir),
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

        val userData = ShellScript()
        userData.addInlineSource(UtilsLibrary)
        userData.addInlineSource(AptLibrary)
        userData.addInlineSource(CurlLibrary)
        userData.addInlineSource(LogLibrary)
        userData.addInlineSource(PackageLibrary)
        userData.addCommand(PackageLibrary.UpdateRepositories())
        userData.addCommand(PackageLibrary.UpdateSystem())

        userData.addInlineSource(StorageLibrary)
        userData.addCommand(StorageLibrary.Mount(dataDevice, storageMount))

        userData.addInlineSource(CaddyLibrary)
        userData.addCommand(CaddyLibrary.Install())
        userData.addCommand(MkDir(caddyDataDir, "caddy"))
        userData.addCommand(
            WriteFile(
                caddyConfig.render().toByteArray(),
                "/etc/caddy/Caddyfile",
                FilePermissions.RW_R__R__,
            ),
        )
        userData.addCommand(SystemDLibrary.Restart("caddy"))

        val garageFsConfig =
            GarageFsConfig(
                garageFsDataDir,
                rpcSecret,
                adminToken,
                metricsToken,
                "s3.$serviceRootDomain",
                "s3-web.$serviceRootDomain",
            )

        val garageFsSystemDConfig =
            SystemDService(
                "garage",
                Unit("Garage Data Store"),
                Service(
                    listOf("/usr/local/bin/garage", "server"),
                    environment = mapOf("RUST_LOG" to "garage=info", "RUST_BACKTRACE" to "1"),
                    limitNOFILE = 42000,
                    stateDirectory = "garage",
                ),
                Install(),
            )

        userData.addInlineSource(GarageLibrary)
        userData.addCommand(GarageLibrary.Install())
        userData.addCommand(
            WriteFile(
                garageFsConfig.render().toByteArray(),
                "/etc/garage.toml",
                FilePermissions.RW_R__R__,
            ),
        )
        userData.addCommand(MkDir(garageFsConfig.dataDir))
        userData.addCommand(MkDir(garageFsConfig.metaDataDir))

        userData.installSystemDUnit(garageFsSystemDConfig)
        userData.addCommand(SystemDLibrary.Restart("garage"))

        userData.resticBackup(serviceName, backupConfiguration, serviceDataDir)

        return userData.render()
    }
}
