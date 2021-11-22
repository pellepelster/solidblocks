package de.solidblocks.core

interface IInfrastructureResource<ResourceType, RuntimeType> : IResource {

    fun getHealthCheck(): (() -> Boolean)? = null
}
