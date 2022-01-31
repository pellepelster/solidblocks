package de.solidblocks.cloud.tenants.api

// fun TenantEntity.fromEntity() = TenantResourceResponse(reference = this.toReference())

data class TenantResourceResponse(
    val reference: TenantResourceReference
)
