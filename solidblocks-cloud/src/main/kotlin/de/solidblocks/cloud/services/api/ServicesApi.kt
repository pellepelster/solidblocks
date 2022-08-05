package de.solidblocks.cloud.services.api

import de.solidblocks.cloud.api.CloudApiHttpServer
import de.solidblocks.cloud.api.email
import de.solidblocks.cloud.api.jsonRequest
import de.solidblocks.cloud.api.jsonResponse
import de.solidblocks.cloud.services.ServicesManager
import io.vertx.ext.web.RoutingContext

class ServicesApi(val cloudApiHttpServer: CloudApiHttpServer, val servicesManager: ServicesManager) {

    init {
        cloudApiHttpServer.configureSubRouter("/api/v1/services", configure = { router ->
            router.get("/catalog").handler(this::catalog)
            router.get().handler(this::list)
            router.post().handler(this::create)
        })
    }

    fun catalog(rc: RoutingContext) {
        rc.jsonResponse(servicesManager.serviceCatalog())
    }

    fun list(rc: RoutingContext) {
        rc.jsonResponse(
            ServicesResponse(
                servicesManager.list(rc.email()).map {
                    it.toResponse()
                }
            )
        )
    }

    fun create(rc: RoutingContext) {
        val request = rc.jsonRequest(ServiceCreateRequest::class.java)

        val result = servicesManager.create(rc.email(), request.name, request.type)

        if (result.hasErrors()) {
            rc.jsonResponse(ServiceCreateResponse(messages = result.messages), 422)
            return
        }

        rc.jsonResponse(ServiceCreateResponse(result.data!!.service.toResponse()), 201)
    }
}
