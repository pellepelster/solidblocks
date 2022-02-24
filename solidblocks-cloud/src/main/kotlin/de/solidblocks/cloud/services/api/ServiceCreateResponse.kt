package de.solidblocks.cloud.services.api

import de.solidblocks.base.api.MessageResponse

data class ServiceCreateResponse(
    val environment: ServiceResponse? = null,
    val messages: List<MessageResponse> = emptyList()
)
