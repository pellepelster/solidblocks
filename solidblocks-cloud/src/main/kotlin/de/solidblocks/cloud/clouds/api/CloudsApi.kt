package de.solidblocks.cloud.clouds.api

import de.solidblocks.cloud.api.CloudApiHttpServer
import de.solidblocks.cloud.api.email
import de.solidblocks.cloud.api.jsonResponse
import de.solidblocks.cloud.clouds.CloudsManager
import de.solidblocks.cloud.environments.api.toResponse
import io.vertx.ext.web.RoutingContext
import java.util.*

class CloudsApi(cloudApiHttpServer: CloudApiHttpServer, val cloudsManager: CloudsManager) {

    init {
        cloudApiHttpServer.configureSubRouter("/api/v1/clouds", configure = { router ->
            router.get("/:id").handler(this::get)
            router.get().handler(this::list)
        })
    }

    fun list(rc: RoutingContext) {
        rc.jsonResponse(
            CloudsResponseWrapper(
                cloudsManager.listClouds(rc.email()).map {
                    CloudResponse(it.id, it.name)
                }
            )
        )
    }

    fun get(rc: RoutingContext) {

        val id = try {
            UUID.fromString(rc.pathParam("id"))
        } catch (e: IllegalArgumentException) {
            rc.jsonResponse(CloudResponseWrapper(), 400)
            return
        }

        val cloud = cloudsManager.getCloud(rc.email(), id)

        if (cloud == null) {
            rc.jsonResponse(CloudResponseWrapper(), 404)
            return
        }

        val environments = cloudsManager.cloudEnvironments(rc.email(), id)
        rc.jsonResponse(CloudResponseWrapper(cloud.toResponse(environments.map { it.toResponse() })))
    }
}
