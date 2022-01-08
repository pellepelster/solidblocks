package de.solidblocks.base.lookups

import de.solidblocks.core.IResourceLookup

class Base64Encode(val datasource: IResourceLookup<String>) : IResourceLookup<String> {
    override val name: String
        get() = "base64.${datasource.name}"
}
