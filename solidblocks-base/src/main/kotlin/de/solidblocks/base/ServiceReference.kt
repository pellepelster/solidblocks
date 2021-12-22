package de.solidblocks.base

data class ServiceReference(val cloud: String, val environment: String, val service: String) {
    fun toEnvironment() = EnvironmentReference(cloud, environment)
}
