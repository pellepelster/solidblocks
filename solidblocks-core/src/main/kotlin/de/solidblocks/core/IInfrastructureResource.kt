package de.solidblocks.core

interface IInfrastructureResource<ResourceType, RuntimeType> : IResource {
    fun getParents(): List<IInfrastructureResource<*, *>> = emptyList()
    fun getParentDataSources(): List<IDataSource<*>> = emptyList()
    fun getHealthCheck(): ((ResourceType) -> Boolean)? = null
}
