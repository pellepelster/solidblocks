package de.solidblocks.cloud.environments.api

import de.solidblocks.cloud.api.ResourceReference
import de.solidblocks.cloud.model.entities.EnvironmentEntity

data class EnvironmentResponse(val reference: EnvironmentResourceReference) {
    companion object {
        fun resourceReference(environment: EnvironmentEntity) = ResourceReference(environment.id, environment.name)
    }
}
