package de.solidblocks.cli.hetzner.api.model

import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
enum class HetznerApiErrorType {
    FORBIDDEN,
    UNAUTHORIZED,
    INVALID_INPUT,
    METHOD_NOT_ALLOWED,
    JSON_ERROR,
    LOCKED,
    NOT_FOUND,
    RATE_LIMIT_EXCEEDED,
    TARGET_ALREADY_DEFINED,
    RESOURCE_IN_USE,
    SERVER_NOT_STOPPED,
    RESOURCE_LIMIT_EXCEEDED,
    RESOURCE_UNAVAILABLE,
    SERVER_ERROR,
    SERVICE_ERROR,
    UNIQUENESS_ERROR,
    PROTECTED,
    MAINTENANCE,
    CONFLICT,
    UNSUPPORTED_ERROR,
    TOKEN_READONLY,
    UNAVAILABLE,
}

@Serializable
data class HetznerApiErrorWrapper(val error: HetznerApiError)

@Serializable
data class HetznerApiError(
    val message: String,
    val code: HetznerApiErrorType,
    val details: HetznerApiErrorDetails? = null
)

@Serializable
data class HetznerApiErrorDetails(val fields: List<HetznerApiErrorDetailsField>? = null)

@Serializable
data class HetznerApiErrorDetailsField(val name: String, val messages: List<String>)

class HetznerApiException(val error: HetznerApiError, val url: Url) : RuntimeException()
