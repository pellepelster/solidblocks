package de.solidblocks.cloud.clouds.api

import de.solidblocks.cloud.api.CloudApiHttpServer
import de.solidblocks.cloud.api.email
import de.solidblocks.cloud.api.jsonResponse
import de.solidblocks.cloud.clouds.CloudsManager
import de.solidblocks.cloud.environments.EnvironmentsManager
import io.vertx.ext.web.RoutingContext

class CloudsApi(cloudApiHttpServer: CloudApiHttpServer, val cloudsManager: CloudsManager, val environmentsManager: EnvironmentsManager) {

    init {
        cloudApiHttpServer.configureSubRouter("/api/v1/clouds", configure = { router ->
            router.get().handler(this::get)
        })
    }

    fun get(rc: RoutingContext) {
        rc.jsonResponse(CloudsResponseWrapper(cloudsManager.listClouds(rc.email()).map { it.toResponse() }))
    }
}
