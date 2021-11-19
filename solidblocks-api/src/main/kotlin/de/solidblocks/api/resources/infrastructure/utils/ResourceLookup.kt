package de.solidblocks.api.resources.infrastructure.utils

import de.solidblocks.core.IDataSource
import de.solidblocks.core.IInfrastructureResource

class ResourceLookup<RuntimeType>(val resource: IInfrastructureResource<*, *>, val call: (RuntimeType) -> String) : IDataSource<String> {
    override fun name(): String {
        return "resource.lookup.${resource.name()}"
    }
}
