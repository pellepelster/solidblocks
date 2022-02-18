package de.solidblocks.cloud.services.api

import de.solidblocks.cloud.model.entities.ServiceEntity
import java.util.*

fun ServiceEntity.toResponse() = ServiceResponse(this.id, this.name)

data class ServiceResponse(val id: UUID, val name: String)
