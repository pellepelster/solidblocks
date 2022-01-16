package de.solidblocks.cloud.model

object ErrorCodes {
    const val UNAUTHORIZED = "unauthorized"

    object CLOUD {
        const val UNKNOWN_DOMAIN = "cloud_unknown_domain"
        const val HOSTNAME_MISSING = "cloud_no_hostname_found"
    }

    object ENVIRONMENT {
        const val NOT_FOUND = "environment_not_found"
    }

    object TENANT {
        const val DUPLICATE = "tenant_duplicate"
    }
}
