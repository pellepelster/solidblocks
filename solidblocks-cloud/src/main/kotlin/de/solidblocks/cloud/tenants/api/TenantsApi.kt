package de.solidblocks.cloud.tenants.api

import de.solidblocks.base.api.MessageResponse
import de.solidblocks.cloud.api.*
import de.solidblocks.cloud.tenants.TenantsManager
import io.vertx.ext.web.RoutingContext

class TenantsApi(val cloudApiHttpServer: CloudApiHttpServer, val tenantsManager: TenantsManager) {

    init {
        cloudApiHttpServer.configureSubRouter("/api/v1/tenants", configure = { router ->
            router.post().handler(this::create)
            router.post("/validate").handler(this::validate)
            router.get().handler(this::list)
        })
    }

    fun list(rc: RoutingContext) {
        val tenants = tenantsManager.listTenantsForUser(rc.email())

        rc.jsonResponse(TenantsResponse(tenants.map { it.toResponse() }))
    }

    fun create(rc: RoutingContext) {
        val request = rc.jsonRequest(TenantCreateRequest::class.java)

        val validateResult = tenantsManager.validate(rc.email(), request)

        if (validateResult.hasErrors()) {
            rc.jsonResponse(TenantCreateResponse(messages = validateResult.messages), 422)
            return
        }

        val result = tenantsManager.createTenantForDefaultEnvironment(rc.email(), request)

        if (result.data == null) {
            rc.jsonResponse(GenericApiResponse(), 500)
        }

        rc.jsonResponse(
            TenantCreateResponse(tenant = result.data!!.toResponse()), 201
        )
    }

    fun validate(rc: RoutingContext) {
        val request = rc.jsonRequest(TenantCreateRequest::class.java)

        val result = tenantsManager.validate(rc.email(), request)

        rc.jsonResponse(
            ValidateResponse(
                result.messages.map {
                    MessageResponse(code = it.code)
                }
            )
        )
    }
}
