package de.solidblocks.cloud.model.entities

import java.util.*

data class UserEntity(
        val id: UUID,
        val email: String,
        val salt: String,
        val password: String,
        val admin: Boolean = false,
        val cloud: CloudEntity? = null,
        val environment: EnvironmentEntity? = null,
        val tenant: TenantEntity? = null
) {
    fun scope(): Scope {

        if (cloud != null) {
            return Scope.cloud
        }

        if (environment != null) {
            return Scope.environment
        }

        if (tenant != null) {
            return Scope.tenant
        }

        if (admin) {
            return Scope.root
        }

        throw RuntimeException("unable to determine scope for user '${email}'")
    }
}
