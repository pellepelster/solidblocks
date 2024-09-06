package de.solidblocks.cli.hetzner

import de.solidblocks.cli.utils.logError
import de.solidblocks.cli.utils.logInfo
import kotlinx.coroutines.runBlocking

class HetznerNuker(hcloudToken: String) {

    val api = HetznerApi(hcloudToken)

    fun run(doNuke: Boolean) = runBlocking {
        if (doNuke) {
            api.volumes.list().forEach {
                if (it.server != null) {
                    api.waitFor({
                        logInfo("detaching volume ${it.logText()} from server ${it.server}")
                        api.volumes.detach(it.id)
                    }, {
                        api.volumes.action(it)
                    })
                }
            }
        }

        listOf(
            api.servers,
            api.volumes,
            api.networks,
            api.sshKeys,
            api.certificates,
            api.firewalls,
            api.floatingIps,
            api.loadBalancers,
            api.primaryIps,
            api.placementGroups,
        ).forEach { resourceApi ->
            resourceApi.list().forEach {
                if (doNuke) {

                    if (resourceApi is HetznerProtectedResourceApi) {
                        api.waitFor({
                            logInfo("disabling protection for ${it.logText()}")
                            resourceApi.changeProtection(it.id, false)
                        }, {
                            resourceApi.action(it)
                        })
                    }

                    if (resourceApi is HetznerAssignedResourceApi) {
                        api.waitFor({
                            logInfo("unassigning ${it.logText()}")
                            resourceApi.unassign(it.id)
                        }, {
                            resourceApi.action(it)
                        })
                    }

                    if (resourceApi is HetznerSimpleResourceApi) {
                        logInfo("deleting ${it.logText()}")
                        if (!resourceApi.delete(it.id)) {
                            logError("deleting ${it.logText()} failed")
                        }
                    }

                    if (resourceApi is HetznerComplexResourceApi) {
                        logInfo("deleting ${it.logText()}")
                        api.waitFor({
                            resourceApi.delete(it.id)
                        }, {
                            resourceApi.action(it)
                        })
                    }

                } else {
                    logInfo("would delete ${it.logText()}")
                }
            }
        }
    }
}