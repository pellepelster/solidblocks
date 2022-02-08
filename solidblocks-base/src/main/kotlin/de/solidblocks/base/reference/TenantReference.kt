package de.solidblocks.base.reference

open class TenantReference(cloud: String, environment: String, val tenant: String) :
    EnvironmentReference(cloud, environment) {

    fun toService(service: String) = ServiceReference(cloud, environment, tenant, service)

    fun toUser(user: String) = UserReference(cloud, environment, tenant, user)

    fun toEnvironmentService(service: String) = EnvironmentServiceReference(cloud, environment, service)

    override fun toString(): String {
        return "$cloud/$environment/$tenant"
    }
}
