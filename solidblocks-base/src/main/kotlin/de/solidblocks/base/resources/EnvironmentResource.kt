package de.solidblocks.base.resources

open class EnvironmentResource(cloud: String, val environment: String) : CloudResource(cloud) {
    fun toTenant(tenant: String) = TenantResource(cloud, environment, tenant)
}
