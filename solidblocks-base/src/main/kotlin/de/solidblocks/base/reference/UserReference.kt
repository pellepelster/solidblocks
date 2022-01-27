package de.solidblocks.base.reference

open class UserReference(cloud: String, environment: String, tenant: String, val email: String) :
    TenantReference(cloud, environment, tenant)
