package de.solidblocks.base.resources

open class TenantResource(cloud: String, environment: String, val tenant: String) :
    EnvironmentResource(cloud, environment) {

    fun toService(service: String) = ServiceResource(cloud, environment, tenant, service)

    fun toUser(user: String) = UserResource(cloud, environment, tenant, user)

    fun toEnvironmentService(service: String) = EnvironmentServiceResource(cloud, environment, service)
}
