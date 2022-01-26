package de.solidblocks.cloud.tenants.api

import de.solidblocks.cloud.api.ResourceReference
import de.solidblocks.cloud.clouds.api.toResourceReference
import de.solidblocks.cloud.environments.api.toResourceReference
import de.solidblocks.cloud.model.entities.TenantEntity

fun TenantEntity.toResourceReference() = ResourceReference(this.id, this.name)

fun TenantEntity.toReference() = TenantResourceReference(
    cloud = this.environment.cloud.toResourceReference(),
    environment = this.environment.toResourceReference(),
    tenant = this.toResourceReference()
)

data class TenantResourceReference(
    val cloud: ResourceReference,
    val environment: ResourceReference,
    val tenant: ResourceReference
)
