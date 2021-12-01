package de.solidblocks.api.resources.infrastructure.utils

import de.solidblocks.core.IResourceLookup
import java.util.*

class CustomDataSource(val id: UUID = UUID.randomUUID(), val content: () -> String) : IResourceLookup<String> {
    override fun id(): String {
        return id.toString()
    }
}
