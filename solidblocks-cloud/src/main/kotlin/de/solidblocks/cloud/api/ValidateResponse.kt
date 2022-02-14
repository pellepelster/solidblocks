package de.solidblocks.cloud.api

import de.solidblocks.base.api.MessageResponse

data class ValidateResponse(val messages: List<MessageResponse> = emptyList())
