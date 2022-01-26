package de.solidblocks.cloud.environments.api

import de.solidblocks.cloud.api.ResourceReference
import de.solidblocks.cloud.model.entities.EnvironmentEntity

fun EnvironmentEntity.toResourceReference() = ResourceReference(this.id, this.name)

data class EnvironmentResourceReference(val cloud: ResourceReference, val environment: ResourceReference)
