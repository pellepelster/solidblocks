package de.solidblocks.core

interface IInfrastructureResource<RuntimeType> : IResource {
    fun getParents(): List<IInfrastructureResource<*>> = emptyList()
    fun getParentDataSources(): List<IDataSource<*>> = emptyList()
}
