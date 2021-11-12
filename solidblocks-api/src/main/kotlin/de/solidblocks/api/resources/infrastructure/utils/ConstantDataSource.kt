package de.solidblocks.api.resources.infrastructure.utils

import de.solidblocks.core.IDataSource

class ConstantDataSource(val content: String) : IDataSource<String> {
    override fun name(): String {
        return "constant.$content"
    }
}
