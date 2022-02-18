package de.solidblocks.cloud.environments.api

import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.cloud.tenants.api.TenantResponse
import java.util.*

fun EnvironmentEntity.toResponse(tenants: List<TenantResponse>) = EnvironmentResponse(this.id, this.name, tenants)

fun EnvironmentEntity.toResponse() = EnvironmentResponse(this.id, this.name)

data class EnvironmentResponse(
    val id: UUID,
    val name: String,
    val tenants: List<TenantResponse> = emptyList()
)
