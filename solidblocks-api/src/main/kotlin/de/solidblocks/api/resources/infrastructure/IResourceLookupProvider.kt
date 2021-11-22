package de.solidblocks.api.resources.infrastructure

import de.solidblocks.core.IResourceLookup
import de.solidblocks.core.Result

interface IResourceLookupProvider<ResourceLookupType : IResourceLookup<T>, T> {

    fun lookup(lookup: ResourceLookupType): Result<T>

    fun getLookupType(): Class<*>
}
