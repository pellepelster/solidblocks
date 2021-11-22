package de.solidblocks.api.resources.infrastructure.utils

import de.solidblocks.core.IResourceLookup

class Base64Encode(val datasource: IResourceLookup<String>) : IResourceLookup<String> {
    override fun name(): String {
        return "base64.${datasource.name()}"
    }
}
