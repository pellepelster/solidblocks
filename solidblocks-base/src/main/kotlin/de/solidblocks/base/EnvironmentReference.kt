package de.solidblocks.base

data class EnvironmentReference(val cloud: String, val environment: String) {

    fun toService(service: String): ServiceReference =
        ServiceReference(cloud, environment, service)

    fun toTenant(tenant: String) = TenantReference(cloud, environment, tenant)

    fun toCloud() = CloudReference(cloud)

    fun toEnvironment() = EnvironmentReference(cloud, environment)
}
