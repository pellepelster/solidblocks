package de.solidblocks.cloud.model.entities

import java.util.*

data class ServiceEntity(
    val id: UUID,
    val name: String,
    val tenant: TenantEntity,
    val configValues: List<CloudConfigValue>
)
