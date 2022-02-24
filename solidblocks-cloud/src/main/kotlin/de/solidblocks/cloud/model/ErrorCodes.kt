package de.solidblocks.cloud.model

object ErrorCodes {

    const val UNAUTHORIZED = "unauthorized"
    const val MANDATORY = "mandatory"
    const val DUPLICATE = "duplicate"

    object CLOUD {
        // const val NOT_FOUND = "environment_not_found"
        const val UNKNOWN_DOMAIN = "cloud_unknown_domain"
        const val HOSTNAME_MISSING = "cloud_no_hostname_found"
    }

    object SERVICES {
        const val NO_TENANT = "services_no_tenant"
        const val USER_NOT_FOUND = "services_user_not_found"
        const val INVALID_TYPE = "services_invalid_type"
    }

    object ENVIRONMENT {

        const val CREATE_FAILED = "create_failed"
        const val DEFAULT_NOT_FOUND = "default_environment_not_found"
        const val NOT_FOUND = "environment_not_found"
        const val INVALID = "invalid"
        const val DEFAULT_CLOUD_NOT_FOUND = "default_cloud_not_found"
    }


    object TENANT {
        const val CREATE_FAILED = "create_failed"
        const val DEFAULT_ENVIRONMENT_NOT_FOUND = "default_environment_not_found"
        const val INVALID = "invalid"
    }

    object LOGIN {
        const val INVALID_CREDENTIALS = "invalid_credentials"
    }
}
