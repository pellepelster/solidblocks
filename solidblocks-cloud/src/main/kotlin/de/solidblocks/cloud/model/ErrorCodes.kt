package de.solidblocks.cloud.model

object ErrorCodes {
    const val UNAUTHORIZED = "unauthorized"

    object ENVIRONMENT {
        const val NOT_FOUND = "environment_not_found"
    }

    object TENANT {
        const val DUPLICATE = "tenant_duplicate"
    }
}
