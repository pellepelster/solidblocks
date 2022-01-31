package de.solidblocks.cloud.tenants.api

import de.solidblocks.cloud.api.*
import de.solidblocks.cloud.tenants.TenantsManager
import io.vertx.ext.web.RoutingContext

class TenantsApi(val cloudApiHttpServer: CloudApiHttpServer, val tenantsManager: TenantsManager) {

    init {
        cloudApiHttpServer.configureSubRouter("/api/v1/tenants", configure = { router, auth ->
            router.post().handler(this::create)
        })

        cloudApiHttpServer.configureSubRouter("/api/v1/tenants", configure = { router ->
            router.post("/validate").handler(this::validate)
            router.get().handler(this::list)
        })
    }

    fun list(rc: RoutingContext) {
        val email = rc.user().principal().getString("email")

        val tenants = tenantsManager.listTenantsForUser(email)

        rc.jsonResponse(TenantsResponse(tenants.map { it.toResponse() }))
    }

    fun create(rc: RoutingContext) {
        val request = rc.jsonRequest(TenantCreateRequest::class.java)
        val validateResult = tenantsManager.validate(request)

        if (validateResult.hasErrors()) {
            rc.jsonResponse(TenantCreateResponse(messages = validateResult.messages), 422)
            return
        }

        // TODO generate password
        val result = tenantsManager.createTenantForDefaultEnvironment(request.tenant!!, request.email!!, "password")

        if (result.data == null) {
            rc.jsonResponse(GenericApiResponse(), 500)
        }

        rc.jsonResponse(
            TenantCreateResponse(tenant = result.data!!.toResponse()), 201
        )
    }

    fun validate(rc: RoutingContext) {
        val request = rc.jsonRequest(TenantCreateRequest::class.java)
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
