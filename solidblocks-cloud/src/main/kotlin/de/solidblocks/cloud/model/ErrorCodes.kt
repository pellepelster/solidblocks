package de.solidblocks.cloud.model

object ErrorCodes {
    const val UNAUTHORIZED = "unauthorized"

    const val MANDATORY = "mandatory"

    object CLOUD {
        const val UNKNOWN_DOMAIN = "cloud_unknown_domain"
        const val HOSTNAME_MISSING = "cloud_no_hostname_found"
    }

    object ENVIRONMENT {
        const val NOT_FOUND = "environment_not_found"
    }

    object TENANT {
        const val ENVIRONMENT_NOT_FOUND = "environment_not_found"
        const val DUPLICATE = "duplicate"
        const val INVALID = "invalid"
    }

    object LOGIN {
        const val INVALID_CREDENTIALS = "invalid_credentials"
    }
}
