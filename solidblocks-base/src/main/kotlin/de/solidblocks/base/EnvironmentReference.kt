package de.solidblocks.base

data class EnvironmentReference(val cloud: String, val environment: String) {
    fun toService(service: String): ServiceReference =
        ServiceReference(cloud, environment, service)
}
