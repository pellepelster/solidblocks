package de.solidblocks.cloud.tenants.api

import de.solidblocks.cloud.model.entities.TenantEntity

fun TenantEntity.fromEntity() = TenantResourceResponse(reference = this.toReference())

data class TenantResourceResponse(
    val reference: TenantResourceReference
)
