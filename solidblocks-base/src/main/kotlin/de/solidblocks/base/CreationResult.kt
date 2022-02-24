package de.solidblocks.base

import de.solidblocks.base.api.MessageResponse
import de.solidblocks.base.api.messageResponses
import java.util.*
import kotlin.reflect.KProperty1

fun <T> String.creationResult(attribute: KProperty1<*, *>? = null) = CreationResult<T>(messages = this.messageResponses(attribute))

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
