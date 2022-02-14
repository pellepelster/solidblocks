package de.solidblocks.cloud.clouds.api

import de.solidblocks.cloud.api.CloudApiHttpServer
import de.solidblocks.cloud.api.jsonResponse
import de.solidblocks.cloud.clouds.CloudsManager
import de.solidblocks.cloud.environments.EnvironmentsManager
import io.vertx.ext.web.RoutingContext

class CloudsApi(cloudApiHttpServer: CloudApiHttpServer, val cloudsManager: CloudsManager, val environmentsManager: EnvironmentsManager) {

    init {
        cloudApiHttpServer.createSubRouter("/api/v1/clouds").get().handler(this::get)
        cloudApiHttpServer.createSubRouter("/api/v1/clouds/info").get().handler(this::info)
    }

    fun get(rc: RoutingContext) {
        val email = rc.user().principal().getString("email")
        rc.jsonResponse(CloudsResponseWrapper(cloudsManager.listCloudsForUser(email).map { it.toResponse() }))
    }

    fun info(rc: RoutingContext) {
        val email = rc.user().principal().getString("email")

        val clouds = cloudsManager.listCloudsForUser(email)

        for (cloud in clouds) {
            // /environmentsManager.ge()
        }
    }

}
