package de.solidblocks.api.resources.infrastructure.utils

import de.solidblocks.core.IDataSource

class Base64Encode(val datasource: IDataSource<String>) : IDataSource<String> {
    override fun name(): String {
        return "base64.${datasource.name()}"
    }
}
