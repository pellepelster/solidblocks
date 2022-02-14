package de.solidblocks.cloud.tenants.api

data class TenantCreateRequest(val tenant: String?, val email: String?, val password: String?)
