package de.solidblocks.cloud.services.api

import de.solidblocks.cloud.api.CloudApiHttpServer
import de.solidblocks.cloud.api.jsonResponse
import io.vertx.ext.web.RoutingContext

class ServicesApi(val cloudApiHttpServer: CloudApiHttpServer) {

    init {
        cloudApiHttpServer.configureSubRouter("/api/v1/services/catalog", configure = { router ->
            router.post().handler(this::catalog)
        })
    }

    fun catalog(rc: RoutingContext) {
        val email = rc.user().principal().getString("email")

        rc.jsonResponse(CatalogResponse(listOf(CatalogItemResponse("helloworld", "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua."))))
    }

}
