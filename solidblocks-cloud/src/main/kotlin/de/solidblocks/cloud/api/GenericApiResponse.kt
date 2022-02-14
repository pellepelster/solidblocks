package de.solidblocks.cloud.api

import de.solidblocks.base.api.MessageResponse

data class GenericApiResponse(val messages: List<MessageResponse> = emptyList())
