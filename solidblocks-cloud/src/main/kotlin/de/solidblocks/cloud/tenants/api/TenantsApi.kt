package de.solidblocks.cloud.tenants.api

import de.solidblocks.cloud.api.*
import de.solidblocks.cloud.tenants.TenantsManager
import io.vertx.ext.web.RoutingContext

class TenantsApi(val cloudApiHttpServer: CloudApiHttpServer, val tenantsManager: TenantsManager) {

    init {
        cloudApiHttpServer.addUnprotectedRouter("/api/v1/tenants").post().handler(this::create)
        cloudApiHttpServer.addUnprotectedRouter("/api/v1/tenants").post("/validate").handler(this::validate)
    }

    fun create(rc: RoutingContext) {
        val request = rc.jsonRequest(TenantCreateRequest::class.java)

        val result = tenantsManager.create(request.tenant, request.email, request.password)

        rc.jsonResponse(
            ValidateResponse(
                result.messages.map {
                    MessageResponse((it.code))
                }
            )
        )
    }

    fun validate(rc: RoutingContext) {
        val request = rc.jsonRequest(TenantCreateRequest::class.java)
        val result = tenantsManager.validate(request.tenant, request.email, request.password)

        rc.jsonResponse(
            ValidateResponse(
                result.messages.map {
                    MessageResponse((it.code))
                }
            )
        )
    }
}
