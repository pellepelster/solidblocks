package de.solidblocks.cloud.model

data class ValidationResult(val messages: List<ValidationMessage>) {

    companion object {
        fun ok() = ValidationResult(emptyList())

        fun error(code: String) = ValidationResult(listOf(ValidationMessage((code))))
    }
}
