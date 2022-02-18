package de.solidblocks.cloud.tenants.api

import de.solidblocks.base.api.MessageResponse
import de.solidblocks.cloud.api.*
import de.solidblocks.cloud.services.api.toResponse
import de.solidblocks.cloud.tenants.TenantsManager
import io.vertx.ext.web.RoutingContext
import java.util.*

class TenantsApi(val cloudApiHttpServer: CloudApiHttpServer, val tenantsManager: TenantsManager) {

    init {
        cloudApiHttpServer.configureSubRouter("/api/v1/tenants", configure = { router ->
            router.get("/:id").handler(this::get)
            router.get().handler(this::list)
            router.post().handler(this::create)
            router.post("/validate").handler(this::validate)
        })
    }

    fun get(rc: RoutingContext) {

        val id = try {
            UUID.fromString(rc.pathParam("id"))
        } catch (e: IllegalArgumentException) {
            rc.jsonResponse(TenantResponseWrapper(), 400)
            return
        }

        val tenant = tenantsManager.getTenant(rc.email(), id)

        if (tenant == null) {
            rc.jsonResponse(TenantResponseWrapper(), 404)
            return
        }

        val services = tenantsManager.tenantServices(rc.email(), tenant.id)

        rc.jsonResponse(TenantResponseWrapper(tenant.toResponse(services.map { it.toResponse() })))
    }

    fun list(rc: RoutingContext) {
        val tenants = tenantsManager.listTenants(rc.email())

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
