package de.solidblocks.cloud.clouds.api

import de.solidblocks.cloud.model.entities.CloudEntity

fun CloudEntity.toResponse() = CloudResponse(reference = this.toReference())

data class CloudResponse(val reference: CloudResourceReference)
