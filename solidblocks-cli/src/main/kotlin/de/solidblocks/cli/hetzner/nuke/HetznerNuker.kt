package de.solidblocks.cli.hetzner.nuke

import de.solidblocks.cli.utils.logError
import de.solidblocks.cli.utils.logInfo
import de.solidblocks.hetzner.cloud.*
import de.solidblocks.hetzner.cloud.model.*
import de.solidblocks.hetzner.cloud.resources.*
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
                api.dnsZones,
            )
                .map { resourceApi ->
                    if (resourceApi is HetznerDnsZonesApi) {
                        resourceApi.list().flatMap { zone ->
                            api.dnsRrSets(zone.name).list().filter { it.type != RRType.SOA && it.type != RRType.NS }
                                .map { set ->
                                    logInfo("would delete ${set.logText()} from ${zone.logText()}")
                                    1
                                }
                        }.sum()

                    } else {
                        val filter =
                            if (resourceApi is HetznerImagesApi) {
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
        }
            .sum()

    fun nuke() = runBlocking {
        api.dnsZones.list().flatMap { zone ->
            val rrSetsApi = api.dnsRrSets(zone.name)
            rrSetsApi.list().filter { it.type != RRType.SOA && it.type != RRType.NS }
                .map { set ->
                    logInfo("deleting ${set.logText()} from ${zone.logText()}")
                    rrSetsApi.delete(set.name, set.type)
                }
        }

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
                val filter =
                    if (resourceApi is HetznerImagesApi) {
                        mapOf("type" to FilterValue.Equals(ImageType.SNAPSHOT.name.lowercase()))
                    } else {
                        emptyMap()
                    }

                resourceApi.list(filter).forEach {
                    if (it is VolumeResponse && it.server != null) {
                        api.waitForAction(
                            {
                                logInfo("detaching volume ${it.logText()} from server ${it.server}")
                                api.volumes.detach(it.id)
                            },
                            { api.volumes.action(it) },
                        )
                    }

                    if (resourceApi is HetznerProtectedResourceApi<*, *> &&
                        it is HetznerDeleteProtectedResource &&
                        it.protection.delete
                    ) {
                        api.waitForAction(
                            {
                                logInfo("disabling protection for ${it.logText()}")
                                resourceApi.changeDeleteProtection(it.id, false)
                            },
                            { resourceApi.action(it) },
                        )
                    }

                    if (resourceApi is HetznerAssignedResourceApi && it is HetznerAssignedResource) {
                        if (it.isAssigned) {
                            val actionResponse = resourceApi.unassign(it.id)
                            if (actionResponse != null) {
                                api.waitForAction(
                                    {
                                        logInfo("unassigning ${it.logText()}")
                                        actionResponse
                                    },
                                    { resourceApi.action(it) },
                                )
                            }
                        }
                    }

                    if (resourceApi is HetznerDeleteResourceApi<*, *>) {
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

                    if (resourceApi is HetznerDeleteWithActionResourceApi<*, *>) {
                        logInfo("deleting ${it.logText()}")
                        api.waitForAction({ resourceApi.delete(it.id) }, { resourceApi.action(it) })
                    }
                }
            }
    }
}
