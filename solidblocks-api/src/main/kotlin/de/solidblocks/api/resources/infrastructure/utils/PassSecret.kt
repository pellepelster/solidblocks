package de.solidblocks.api.resources.infrastructure.utils

import de.solidblocks.core.IDataSource
import de.solidblocks.core.IInfrastructureResource

class PassSecret(val key: String, val value: String) : IInfrastructureResource<String> {
    override fun name(): String {
        return key
    }

    override fun getParents(): List<IInfrastructureResource<*>> {
        return emptyList()
    }

    override fun getParentDataSources(): List<IDataSource<*>> {
        return emptyList()
    }
}
