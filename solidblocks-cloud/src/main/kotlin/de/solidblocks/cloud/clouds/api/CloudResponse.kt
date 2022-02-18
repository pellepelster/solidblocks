package de.solidblocks.cloud.clouds.api

import de.solidblocks.cloud.environments.api.EnvironmentResponse
import de.solidblocks.cloud.model.entities.CloudEntity
import java.util.*

fun CloudEntity.toResponse() = CloudResponse(id = this.id, name = this.name)

fun CloudEntity.toResponse(environments: List<EnvironmentResponse>) = CloudResponse(id = this.id, name = this.name, environments)

data class CloudResponse(
    val id: UUID,
    val name: String,
    val environments: List<EnvironmentResponse> = emptyList()
)
