package de.solidblocks.cloud.environments.api

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
        val email = rc.user().principal().getString("email")
        val environments = environmentsManager.listEnvironmentsForUser(email)
        rc.jsonResponse(EnvironmentsResponse(environments.map { it.toResponse() }))
    }

    fun create(rc: RoutingContext) {
        val request = rc.jsonRequest(EnvironmentCreateRequest::class.java)
        val validateResult = environmentsManager.validate(request)

        if (validateResult.hasErrors()) {
            rc.jsonResponse(EnvironmentCreateResponse(messages = validateResult.messages), 422)
            return
        }

        // TODO generate password
        val result = tenantsManager.createTenantForDefaultEnvironment(request.tenant!!, request.email!!, "password")

        if (result.data == null) {
            rc.jsonResponse(GenericApiResponse(), 500)
        }

        rc.jsonResponse(
            EnvironmentCreateResponse(tenant = result.data!!.toResponse()), 201
        )
    }

    fun validate(rc: RoutingContext) {
        val request = rc.jsonRequest(EnvironmentCreateRequest::class.java)
        val result = tenantsManager.validate(request)

        rc.jsonResponse(
            ValidateResponse(
                result.messages.map {
                    MessageResponse(code = it.code)
                }
            )
        )
    }
}
