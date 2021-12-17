package de.solidblocks.base

data class ServiceReference(val cloud: String, val environment: String, val service: String) {
    val environmentReference: EnvironmentReference
        get() = EnvironmentReference(cloud, environment)
}
