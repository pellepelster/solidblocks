package de.solidblocks.base.lookups

import de.solidblocks.core.IResource
import de.solidblocks.core.IResourceLookup
import java.util.*

class CustomDataSource(
    override val name: String = UUID.randomUUID().toString(),
    override val parents: Set<IResource> = emptySet(),
    val content: () -> String?
) : IResourceLookup<String>
