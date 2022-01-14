package de.solidblocks.cloud.model

class CreationResult<T>(val data: T? = null, val messages: List<ValidationMessage> = emptyList()) {
    companion object {
        fun <T> error(code: String) = CreationResult<T>(messages = listOf(ValidationMessage((code))))
    }
}
