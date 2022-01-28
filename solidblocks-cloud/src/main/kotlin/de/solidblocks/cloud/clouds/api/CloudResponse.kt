package de.solidblocks.cloud.clouds.api

import de.solidblocks.cloud.model.entities.CloudEntity
import java.util.*

fun CloudEntity.toResponse() = CloudResponse(id = this.id, name = this.name)

data class CloudResponse(val id: UUID, val name: String)
