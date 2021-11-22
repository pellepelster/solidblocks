package de.solidblocks.api.resources.infrastructure.utils

import de.solidblocks.core.IResourceLookup

class ConstantDataSource(val content: String) : IResourceLookup<String> {
    override fun name(): String {
        return "constant.$content"
    }
}