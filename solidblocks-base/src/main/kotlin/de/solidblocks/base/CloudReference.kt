package de.solidblocks.base

fun String.toCloudReference() = CloudReference(this)

data class CloudReference(val cloud: String) {
    fun toEnvironment(environment: String) = EnvironmentReference(cloud, environment)
}
