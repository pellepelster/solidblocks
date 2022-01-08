package de.solidblocks.core

interface IInfrastructureResource<ResourceType, RuntimeType> : IResource {

    val healthCheck: (() -> Boolean)?
        get() = null
}
