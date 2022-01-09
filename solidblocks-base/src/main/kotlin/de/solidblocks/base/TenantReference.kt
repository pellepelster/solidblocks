package de.solidblocks.base

data class TenantReference(val cloud: String, val environment: String, val tenant: String) {
    fun toEnvironment() = EnvironmentReference(cloud, environment)
    fun toCloud() = toEnvironment().toCloud()
}
