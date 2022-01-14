package de.solidblocks.base

open class UserReference(cloud: String, environment: String, tenant: String, val email: String) :
    TenantReference(cloud, environment, tenant)
