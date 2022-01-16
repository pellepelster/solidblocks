package de.solidblocks.base.resources

open class UserResource(cloud: String, environment: String, tenant: String, val email: String) :
    TenantResource(cloud, environment, tenant)
