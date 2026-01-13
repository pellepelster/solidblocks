package de.solidblocks.garagefs

import de.solidblocks.caddy.*
import de.solidblocks.cloudinit.ServiceUserData
import de.solidblocks.cloudinit.model.*
import de.solidblocks.shell.*

class GarageFsUserData(val linuxDevice: String) : ServiceUserData {
    override fun render(): String {

        val storageMount = "/storage/data"
        val caddyConfig =
            CaddyConfig(
                GlobalOptions(
                    FileSystemStorage("${storageMount}/www"),
                    "info@yolo.de",
                    AutoHttps.off
                ),
                listOf(Site(":80"))
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
        userData.addCommand(
            File(
                caddyConfig.render().toByteArray(), "/etc/caddy/Caddyfile", FilePermissions(
                    UserPermission.RWX,
                    GroupPermission.R__,
                    OtherPermission.R__,
                )
            )
        )
        userData.addCommand(SystemdRestartService("caddy"))

        return userData.render()
    }
}