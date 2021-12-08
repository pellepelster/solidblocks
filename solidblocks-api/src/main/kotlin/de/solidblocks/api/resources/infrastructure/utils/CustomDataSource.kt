package de.solidblocks.api.resources.infrastructure.utils

import de.solidblocks.core.IResource
import de.solidblocks.core.IResourceLookup
import java.util.*

class CustomDataSource(val id: UUID = UUID.randomUUID(), val dependencies: List<IResource> = emptyList(), val content: () -> String?) : IResourceLookup<String> {
    override fun id(): String {
        return id.toString()
    }

    override fun getParents(): List<IResource> {
        return dependencies
    }
}
