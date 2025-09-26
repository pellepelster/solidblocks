package de.solidblocks.cli.hetzner.nuke

import de.solidblocks.cli.utils.logError
import de.solidblocks.cli.utils.logInfo
import de.solidblocks.hetzner.cloud.*
import de.solidblocks.hetzner.cloud.model.*
import de.solidblocks.hetzner.cloud.resources.HetznerImagesApi
import de.solidblocks.hetzner.cloud.resources.ImageType
import de.solidblocks.hetzner.cloud.resources.VolumeResponse
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
                    val filter = if (resourceApi is HetznerImagesApi) {
                        mapOf("type" to FilterValue.Equals(ImageType.SNAPSHOT.name.lowercase()))
                    } else {
                        emptyMap()
                    }

                    resourceApi
                        .list(filter)
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
                val filter = if (resourceApi is HetznerImagesApi) {
                    mapOf("type" to FilterValue.Equals(ImageType.SNAPSHOT.name.lowercase()))
                } else {
                    emptyMap()
                }

                resourceApi.list(filter).forEach {
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
                            val actionResponse = resourceApi.unassign(it.id)
                            if (actionResponse != null) {
                                api.waitFor(
                                    {
                                        logInfo("unassigning ${it.logText()}")
                                        actionResponse
                                    },
                                    { resourceApi.action(it) },
                                )
                            }
                        }
                    }

                    if (resourceApi is HetznerDeleteResourceApi<*>) {
                        logInfo("deleting ${it.logText()}")
                        try {
                            if (!resourceApi.delete(it.id)) {
                                logError("deleting ${it.logText()} failed")
                            }
                        } catch (e: HetznerApiException) {
                            if (e.error.code != HetznerApiErrorType.NOT_FOUND) {
                                throw e
                            }
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
