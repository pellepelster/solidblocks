package de.solidblocks.hetzner.nuke

import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import me.tomsdevsn.hetznercloud.objects.general.Action

private fun Action.succesfull(): Boolean = this.finished != null && this.status == "success"


class Nuker(apiToken: String) {

    private val hetznerCloudAPI = HetznerCloudAPI(apiToken)

    fun waitForAction(action: Action) = Waiter.defaultWaiter().waitFor {

        if (action.succesfull()) {
            return@waitFor true
        }

        val actionResult = hetznerCloudAPI.getAction(action.id)
        println("waiting for action '${actionResult.action.command}' to finish, current status is '${actionResult.action.status}'")
        actionResult.action.succesfull()
    }

    fun deleteAllVolumes(doNuke: Boolean) {
        hetznerCloudAPI.volumes.volumes.forEach {

            if (doNuke) {
                println("would delete volume '${it.name}")
            } else {
                println("deleting volume '${it.name}")
                if (it.server != null) {
                    waitForAction(hetznerCloudAPI.detachVolume(it.id).action)
                }

                hetznerCloudAPI.deleteVolume(it.id)
            }
        }
    }

    fun deleteAllServers() {
        hetznerCloudAPI.servers.servers.forEach {
            println("deleting server '${it.name}")
            waitForAction(hetznerCloudAPI.deleteServer(it.id).action)
        }
    }

}