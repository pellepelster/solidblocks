package de.solidblocks.cli.hetzner

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
data class HetznerApiError(val message: String, val code: HetznerApiErrorType)

class HetznerApiException(val error: HetznerApiError, val url: Url) : RuntimeException()
