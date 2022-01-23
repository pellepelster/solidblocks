package de.solidblocks.cloud.model.entities

import de.solidblocks.base.resources.ResourcePermissions
import de.solidblocks.base.resources.parsePermissions
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
    fun scope() = when {
        listOfNotNull(
            cloud,
            environment,
            tenant
        ).size > 1 -> throw RuntimeException("invalid entity for user '${email}'")
        cloud != null -> Scope.cloud
        environment != null -> Scope.environment
        tenant != null -> Scope.tenant
        admin -> Scope.root
        else -> throw RuntimeException("unable to determine scope for user '${email}'")
    }

    fun permissions() = when {
        admin -> listOf("srn:::").parsePermissions()
        scope() == Scope.cloud -> listOf("srn:${cloud!!.name}::").parsePermissions()
        scope() == Scope.environment -> listOf("srn:${environment!!.cloud.name}:${environment.name}:").parsePermissions()
        scope() == Scope.tenant -> listOf("srn:${tenant!!.environment.cloud.name}:${tenant.environment.name}:${tenant.name}").parsePermissions()
        else -> ResourcePermissions()
    }
}
