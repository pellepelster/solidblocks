package de.solidblocks.cloud.services.api

import de.solidblocks.cloud.services.ServiceInstance
import java.util.*

fun ServiceInstance.toResponse() = ServiceResponse(this.service.id, this.service.name, this.service.type)

data class ServiceResponse(val id: UUID, val name: String, val type: String)
