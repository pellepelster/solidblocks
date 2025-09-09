package de.solidblocks.cli.hetzner.nuke

import de.solidblocks.cli.hetzner.api.HetznerApi
import de.solidblocks.cli.hetzner.api.HetznerApiErrorType
import de.solidblocks.cli.hetzner.api.HetznerApiException
import de.solidblocks.cli.hetzner.api.HetznerAssignedResource
import de.solidblocks.cli.hetzner.api.HetznerAssignedResourceApi
import de.solidblocks.cli.hetzner.api.HetznerDeleteResourceApi
import de.solidblocks.cli.hetzner.api.HetznerDeleteWithActionResourceApi
import de.solidblocks.cli.hetzner.api.HetznerProtectedResource
import de.solidblocks.cli.hetzner.api.HetznerProtectedResourceApi
import de.solidblocks.cli.hetzner.api.logText
import de.solidblocks.cli.hetzner.api.resources.VolumeResponse
import de.solidblocks.cli.utils.logError
import de.solidblocks.cli.utils.logInfo
import kotlinx.coroutines.runBlocking

class HetznerNuker(hcloudToken: String) {

    val api = HetznerApi(hcloudToken)

    fun simulate() =
        runBlocking {
            listOf(
                api.volumes,
                api.servers,
                api.networks,
                api.sshKeys,
                api.firewalls,
                api.floatingIps,
                api.loadBalancers,
                api.primaryIps,
                api.placementGroups,
                api.images,
                api.certificates,
            )
                .map { resourceApi ->
                    resourceApi
                        .list()
                        .map {
                            logInfo("would delete ${it.logText()}")
                            1
                        }
                        .sum()
                }
        }
            .sum()

    fun nuke() = runBlocking {
        listOf(
            api.volumes,
            api.servers,
            api.networks,
            api.sshKeys,
            api.firewalls,
            api.floatingIps,
            api.loadBalancers,
            api.primaryIps,
            api.placementGroups,
            api.images,
            api.certificates,
        )
            .forEach { resourceApi ->
                resourceApi.list().forEach {
                    if (it is VolumeResponse && it.server != null) {
                        api.waitFor(
                            {
                                logInfo("detaching volume ${it.logText()} from server ${it.server}")
                                api.volumes.detach(it.id)
                            },
                            { api.volumes.action(it) },
                        )
                    }

                    if (resourceApi is HetznerProtectedResourceApi &&
                        it is HetznerProtectedResource &&
                        it.protection.delete
                    ) {
                        api.waitFor(
                            {
                                logInfo("disabling protection for ${it.logText()}")
                                resourceApi.changeProtection(it.id, false)
                            },
                            { resourceApi.action(it) },
                        )
                    }

                    if (resourceApi is HetznerAssignedResourceApi && it is HetznerAssignedResource) {
                        if (it.isAssigned) {
                            try {
                                api.waitFor(
                                    {
                                        logInfo("unassigning ${it.logText()}")
                                        resourceApi.unassign(it.id)
                                    },
                                    { resourceApi.action(it) },
                                )
                            } catch (e: HetznerApiException) {
                                if (e.error.code != HetznerApiErrorType.NOT_FOUND) {
                                    throw e
                                }
                            }
                        }
                    }

                    if (resourceApi is HetznerDeleteResourceApi<*>) {
                        logInfo("deleting ${it.logText()}")
                        if (!resourceApi.delete(it.id)) {
                            logError("deleting ${it.logText()} failed")
                        }
                    }

                    if (resourceApi is HetznerDeleteWithActionResourceApi) {
                        logInfo("deleting ${it.logText()}")
                        api.waitFor({ resourceApi.delete(it.id) }, { resourceApi.action(it) })
                    }
                }
            }
    }
}
