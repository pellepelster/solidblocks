package de.solidblocks.base.resources

fun List<String>.parsePermissions() = ResourcePermissions(this.mapNotNull { ResourcePermission.parse(it) })

fun String.parsePermissions() = listOf(this).parsePermissions()

data class CloudPermission(val wildcard: Boolean, val cloud: String?)

data class EnvironmentPermission(val wildcard: Boolean, val environment: String?)

data class TenantPermission(val wildcard: Boolean, val tenant: String?)

data class ResourcePermissions(val permissions: List<ResourcePermission> = emptyList()) {

    val isCloudWildcard: Boolean
        get() = permissions.any { it.cloud.wildcard }

    val clouds: List<String>
        get() = permissions.mapNotNull { it.cloud.cloud }

    val isEnvironmentWildcard: Boolean
        get() = permissions.any { it.environment.wildcard }

    val environments: List<String>
        get() = permissions.mapNotNull { it.environment.environment }

    val isTenantWildcard: Boolean
        get() = permissions.any { it.tenant.wildcard }

    val tenants: List<String>
        get() = permissions.mapNotNull { it.tenant.tenant }

    companion object {
        fun parse(permissions: List<String>) =
            ResourcePermissions(permissions.mapNotNull { ResourcePermission.parse(it) })

        fun adminPermissions() = listOf("src:::").parsePermissions()
    }
}

data class ResourcePermission(val cloud: CloudPermission, val environment: EnvironmentPermission, val tenant: TenantPermission) {

    companion object {

        fun parse(permissions: List<String>) = ResourcePermissions(permissions.mapNotNull { parse(it) })

        fun parse(permission: String?): ResourcePermission? {

            if (permission == null) {
                return null
            }

            val parts = permission.split(":")

            if (parts.size != 4) {
                return null
            }

            if (parts[0] != "srn") {
                return null
            }

            val cloud = CloudPermission(wildcard = parts[1].isBlank(), cloud = parts[1].let {
                if (it.isBlank()) {
                    return@let null
                }
                it
            })

            val environment = EnvironmentPermission(wildcard = parts[2].isBlank(), environment = parts[2].let {
                if (it.isBlank()) {
                    return@let null
                }
                it
            })

            val tenant = TenantPermission(wildcard = parts[3].isBlank(), tenant = parts[3].let {
                if (it.isBlank()) {
                    return@let null
                }
                it
            })

            return ResourcePermission(cloud, environment, tenant)
        }
    }

}