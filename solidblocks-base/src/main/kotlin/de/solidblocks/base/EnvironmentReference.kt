package de.solidblocks.base

open class EnvironmentReference(cloud: String, val environment: String) : CloudReference(cloud) {
    fun toTenant(tenant: String) = TenantReference(cloud, environment, tenant)
}
