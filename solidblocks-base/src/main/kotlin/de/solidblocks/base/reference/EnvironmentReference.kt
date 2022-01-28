package de.solidblocks.base.reference

open class EnvironmentReference(cloud: String, val environment: String) : CloudReference(cloud) {
    fun toTenant(tenant: String) = TenantReference(cloud, environment, tenant)

    override fun toString(): String {
        return "$cloud/$environment"
    }
}
