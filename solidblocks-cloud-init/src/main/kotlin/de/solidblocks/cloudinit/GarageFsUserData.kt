package de.solidblocks.cloudinit

import de.solidblocks.shell.AptLibrary
import de.solidblocks.shell.CaddyLibrary
import de.solidblocks.shell.CurlLibrary
import de.solidblocks.shell.FilePermissions
import de.solidblocks.shell.GarageLibrary
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
import de.solidblocks.shell.garagefs.GarageFsConfig
import de.solidblocks.shell.systemd.Install
import de.solidblocks.shell.systemd.Service
import de.solidblocks.shell.systemd.SystemDService
import de.solidblocks.shell.systemd.Unit
import de.solidblocks.shell.systemd.installSystemDUnit
import de.solidblocks.shell.toCloudInit

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

    override fun shellScript(): ShellScript {
        val storageMount = "/storage/data"
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

        val shellScript = ShellScript()

        shellScript.addLibrary(CurlLibrary)
        shellScript.addLibrary(AptLibrary)
        shellScript.addCommand(AptLibrary.UpdateRepositories())
        shellScript.addCommand(AptLibrary.UpdateSystem())

        shellScript.addLibrary(StorageLibrary)
        shellScript.addCommand(StorageLibrary.Mount(dataDevice, storageMount))

        shellScript.addLibrary(CaddyLibrary)
        shellScript.addCommand(CaddyLibrary.Install())
        shellScript.addCommand(MkDir(caddyDataDir, "caddy"))
        shellScript.addCommand(
            WriteFile(
                caddyConfig.render().toByteArray(),
                "/etc/caddy/Caddyfile",
                FilePermissions.RW_R__R__,
            ),
        )
        shellScript.addCommand(SystemDLibrary.Restart("caddy"))

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

        shellScript.addLibrary(GarageLibrary)
        shellScript.addCommand(GarageLibrary.Install())
        shellScript.addCommand(
            WriteFile(
                garageFsConfig.render().toByteArray(),
                "/etc/garage.toml",
                FilePermissions.RW_R__R__,
            ),
        )
        shellScript.addCommand(MkDir(garageFsConfig.dataDir))
        shellScript.addCommand(MkDir(garageFsConfig.metaDataDir))

        shellScript.installSystemDUnit(garageFsSystemDConfig)
        shellScript.addCommand(SystemDLibrary.Restart("garage"))

        shellScript.resticBackup(serviceName, backupConfiguration, serviceDataDir)

        return shellScript
    }
}
