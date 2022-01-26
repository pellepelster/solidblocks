package de.solidblocks.cloud.clouds.api

import de.solidblocks.cloud.api.ResourceReference
import de.solidblocks.cloud.model.entities.CloudEntity

fun CloudEntity.toResourceReference() = ResourceReference(this.id, this.name)

fun CloudEntity.toReference() = CloudResourceReference(cloud = this.toResourceReference())

data class CloudResourceReference(val cloud: ResourceReference)
