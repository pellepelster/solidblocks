package de.solidblocks.cloud.model

import de.solidblocks.cloud.api.MessageResponse
import kotlin.reflect.KProperty1

data class ValidationResult(val messages: List<MessageResponse>) {

    fun hasErrors() = messages.isNotEmpty()

    companion object {
        fun ok() = ValidationResult(emptyList())

        fun error(code: String) = ValidationResult(listOf(MessageResponse(code = code)))

        fun error(attribute: KProperty1<*, *>, code: String) =
            ValidationResult(listOf(MessageResponse(attribute = attribute.name, code = code)))
    }
}
