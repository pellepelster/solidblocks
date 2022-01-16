package de.solidblocks.cloud.clouds.api

import de.solidblocks.cloud.api.CloudApiHttpServer
import de.solidblocks.cloud.clouds.CloudsManager
import io.vertx.ext.web.RoutingContext

class CloudsApi(cloudApiHttpServer: CloudApiHttpServer, val cloudsManager: CloudsManager) {

    init {
        cloudApiHttpServer.addUnprotectedRouter("/api/v1/clouds").get().handler(this::get)
    }

    fun get(rc: RoutingContext) {
    }

    /*
    fun get(rc: RoutingContext) {
        val host = rc.request().getHeader("Host")
        val cloud = cloudsManager.getByHostHeader(host)

        if (cloud == null) {
            rc.jsonResponse(CloudResponseWrapper(messages = ErrorCodes.CLOUD.UNKNOWN_DOMAIN.toMessages()), 404)
            return
        }

        rc.jsonResponse(CloudResponseWrapper(CloudResponse(cloud.name)))
    }*/
}
