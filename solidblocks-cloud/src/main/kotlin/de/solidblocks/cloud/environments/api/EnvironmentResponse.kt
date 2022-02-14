package de.solidblocks.cloud.environments.api

import de.solidblocks.cloud.model.entities.EnvironmentEntity
import java.util.*

fun EnvironmentEntity.toResponse() = EnvironmentResponse(this.id, this.name)

data class EnvironmentResponse(
    val id: UUID,
    val name: String
)