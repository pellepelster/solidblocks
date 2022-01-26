package de.solidblocks.cloud.model

import de.solidblocks.cloud.api.MessageResponse

class CreationResult<T>(val data: T? = null, val messages: List<MessageResponse> = emptyList()) {
    companion object {
        fun <T> error(code: String) = CreationResult<T>(messages = listOf(MessageResponse(code = code)))
    }
}
