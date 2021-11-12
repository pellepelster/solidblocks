package de.solidblocks.core

object NullResource : IInfrastructureResource<Any> {
    override fun getParents(): List<IInfrastructureResource<Any>> {
        return listOf()
    }

    override fun getParentDataSources(): List<IDataSource<*>> {
        return listOf()
    }

    override fun name(): String {
        return "<null resource>"
    }
}
