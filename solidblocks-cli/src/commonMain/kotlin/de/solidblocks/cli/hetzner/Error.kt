package de.solidblocks.cli.hetzner

import kotlinx.serialization.Serializable

@Serializable
enum class HetznerApiErrorType {
    forbidden,
    unauthorized,
    invalid_input,
    method_not_allowed,
    json_error,
    locked,
    not_found,
    rate_limit_exceeded,
    resource_in_use,
    server_not_stopped,
    resource_limit_exceeded,
    resource_unavailable,
    server_error,
    service_error,
    uniqueness_error,
    protected,
    maintenance,
    conflict,
    unsupported_error,
    token_readonly,
    unavailable
}

@Serializable
data class HetznerApiErrorWrapper(val error: HetznerApiError)

@Serializable
data class HetznerApiError(val message: String, val code: HetznerApiErrorType)

class HetznerApiException(val error: HetznerApiError) : RuntimeException()