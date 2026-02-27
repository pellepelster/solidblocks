package de.solidblocks.cloud.api.resources

abstract class InfrastructureResourceLookup<RuntimeType>(name: String, dependsOn: Set<BaseResource>) : BaseResource(name, dependsOn) {
    fun isLookupFor(resource: BaseInfrastructureResource<*>) = resource.lookupType == this::class && resource.name == name
}
