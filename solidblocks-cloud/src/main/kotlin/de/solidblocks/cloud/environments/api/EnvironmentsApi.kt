package de.solidblocks.cloud.environments.api

import de.solidblocks.base.api.MessageResponse
import de.solidblocks.cloud.api.*
import de.solidblocks.cloud.environments.EnvironmentsManager
import io.vertx.ext.web.RoutingContext

class EnvironmentsApi(val cloudApiHttpServer: CloudApiHttpServer, val environmentsManager: EnvironmentsManager) {

    init {
        cloudApiHttpServer.configureSubRouter("/api/v1/environments", configure = { router ->
            router.post("/validate").handler(this::validate)
            router.get().handler(this::list)
            router.post().handler(this::create)
        })
    }

    fun list(rc: RoutingContext) {
        val environments = environmentsManager.listEnvironments(rc.email())
        rc.jsonResponse(EnvironmentsResponse(environments.map { it.toResponse() }))
    }

    fun create(rc: RoutingContext) {
        val request = rc.jsonRequest(EnvironmentCreateRequest::class.java)

        val result = environmentsManager.createEnvironmentForDefaultCloud(rc.email(), request)

        if (result.hasErrors()) {
            rc.jsonResponse(EnvironmentCreateResponse(messages = result.messages), 422)
            return
        }

        if (result.data == null) {
            rc.jsonResponse(GenericApiResponse(), 500)
            return
        }

        rc.jsonResponse(
            EnvironmentCreateResponse(environment = result.data!!.toResponse()), 201
        )
    }

    fun validate(rc: RoutingContext) {
        val request = rc.jsonRequest(EnvironmentCreateRequest::class.java)
        val result = environmentsManager.validate(rc.email(), request)

        rc.jsonResponse(
            ValidateResponse(
                result.messages.map {
                    MessageResponse(code = it.code)
                }
            )
        )
    }
}
