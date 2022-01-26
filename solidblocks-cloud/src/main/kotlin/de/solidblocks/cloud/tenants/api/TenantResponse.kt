package de.solidblocks.cloud.tenants.api

import de.solidblocks.cloud.model.entities.TenantEntity
import java.util.*

// fun TenantEntity.toResponse() = TenantResponse(reference = this.toReference())
fun TenantEntity.toResponse() = TenantResponse(this.id, this.name)

data class TenantResponse(
    val id: UUID,
    val name: String
)
