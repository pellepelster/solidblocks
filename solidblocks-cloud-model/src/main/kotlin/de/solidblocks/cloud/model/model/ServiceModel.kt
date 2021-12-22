package de.solidblocks.cloud.model.model

import java.util.*

data class ServiceModel(
    val id: UUID,
    val name: String,
    val environment: EnvironmentModel
)
