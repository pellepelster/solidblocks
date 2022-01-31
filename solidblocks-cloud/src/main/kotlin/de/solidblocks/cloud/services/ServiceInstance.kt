package de.solidblocks.cloud.services

import de.solidblocks.api.services.ServiceManager
import de.solidblocks.cloud.model.entities.ServiceEntity

data class ServiceInstance(val service: ServiceEntity, val manager: ServiceManager)
