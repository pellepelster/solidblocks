package de.solidblocks.garagefs

import de.solidblocks.cloudinit.ServiceUserData
import de.solidblocks.cloudinit.model.CloudInitUserData
import de.solidblocks.cloudinit.model.Mount

class GarageFsUserData(val linuxDevice: String) : ServiceUserData {
    override fun getUserData(): String {
        val userData = CloudInitUserData()
        userData.addCommand(Mount(linuxDevice, "/storage/data"))
        return userData.render()
    }
}