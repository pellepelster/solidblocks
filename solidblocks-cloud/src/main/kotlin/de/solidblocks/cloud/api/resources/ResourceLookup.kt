package de.solidblocks.cloud.api.resources

interface ResourceLookup<RuntimeType> : Resource {
    fun isLookupFor(resource: InfrastructureResource<RuntimeType>) = this.javaClass.isAssignableFrom(resource.javaClass) && resource.name == name
}
