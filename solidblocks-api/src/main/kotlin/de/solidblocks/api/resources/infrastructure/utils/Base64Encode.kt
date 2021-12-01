package de.solidblocks.api.resources.infrastructure.utils

import de.solidblocks.core.IResourceLookup

class Base64Encode(val datasource: IResourceLookup<String>) : IResourceLookup<String> {
    override fun id(): String {
        return "base64.${datasource.id()}"
    }
}
