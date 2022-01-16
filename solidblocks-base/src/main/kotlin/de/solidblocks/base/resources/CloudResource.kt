package de.solidblocks.base.resources

open class CloudResource(val cloud: String) {
    fun toEnvironment(environment: String) = EnvironmentResource(cloud, environment)
}
