package de.solidblocks.base.lookups

import de.solidblocks.core.IResourceLookup

class ConstantDataSource(val content: String) : IResourceLookup<String> {
    override val name: String = "constant.$content"
}
