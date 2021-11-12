package de.solidblocks.api.resources.infrastructure.utils

import de.solidblocks.core.IDataSource

class CustomDataSource(val content: () -> String) : IDataSource<String> {
    override fun name(): String {
        return "custom"
    }
}
