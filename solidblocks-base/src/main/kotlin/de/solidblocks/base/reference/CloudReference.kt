package de.solidblocks.base.reference

open class CloudReference(val cloud: String) {
    fun toEnvironment(environment: String) = EnvironmentReference(cloud, environment)

    override fun toString(): String {
        return cloud
    }
}
