package de.solidblocks.base.resources

open class ServiceResource(cloud: String, environment: String, tenant: String, val service: String) :
    TenantResource(cloud, environment, tenant) {
    fun asTenant() = TenantResource(cloud, environment, tenant)
}
