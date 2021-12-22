package de.solidblocks.cloud.model.entities

import java.util.*

data class TenantEntity(
    val id: UUID,
    val name: String,
    val environment: EnvironmentEntity
)
