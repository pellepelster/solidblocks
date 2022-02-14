package de.solidblocks.cloud.model

import de.solidblocks.base.CreationResult
import de.solidblocks.base.api.MessageResponse
import de.solidblocks.base.api.messageResponses
import kotlin.reflect.KProperty1

fun <T> ValidationResult.toCreationResult() =
    CreationResult<T>(
        messages = this.messages
    )

data class ValidationResult(val messages: List<MessageResponse>) {

    fun hasErrors() = messages.isNotEmpty()

    companion object {
        fun ok() = ValidationResult(emptyList())

        fun error(code: String) = ValidationResult(listOf(MessageResponse(code = code)))

        fun error(attribute: KProperty1<*, *>, code: String) =
            ValidationResult(code.messageResponses(attribute))
    }
}
