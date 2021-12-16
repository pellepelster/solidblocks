package de.solidblocks.base.lookups

import de.solidblocks.core.IResource
import de.solidblocks.core.IResourceLookup
import java.util.*

class CustomDataSource(val id: UUID = UUID.randomUUID(), val dependencies: Set<IResource> = emptySet(), val content: () -> String?) : IResourceLookup<String> {
    override fun id(): String {
        return id.toString()
    }

    override fun getParents(): Set<IResource> {
        return dependencies
    }
}
