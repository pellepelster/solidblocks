package de.solidblocks.base

import de.solidblocks.base.api.MessageResponse
import java.util.*

class CreationResult<T>(
    val data: T? = null,
    val messages: List<MessageResponse> = emptyList(),
    val actions: List<UUID> = emptyList()
) {

    fun hasErrors() = messages.isNotEmpty()

    companion object {
        fun <T> error(code: String) = CreationResult<T>(messages = listOf(MessageResponse(code = code)))
    }
}
