package de.solidblocks.cloud.environments.api

import de.solidblocks.cloud.api.MessageResponse

data class EnvironmentCreateResponse(
    val environment: EnvironmentResponse? = null,
    val messages: List<MessageResponse> = emptyList()
)
