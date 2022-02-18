package de.solidblocks.cloud.tenants.api

import de.solidblocks.cloud.model.entities.TenantEntity
import de.solidblocks.cloud.services.api.ServiceResponse
import java.util.*

fun TenantEntity.toResponse() = TenantResponse(this.id, this.name)

fun TenantEntity.toResponse(services: List<ServiceResponse>) = TenantResponse(this.id, this.name, services)

data class TenantResponse(
    val id: UUID,
    val name: String,
    val services: List<ServiceResponse> = emptyList()
)
