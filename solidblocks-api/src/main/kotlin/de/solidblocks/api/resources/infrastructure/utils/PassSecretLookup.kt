package de.solidblocks.api.resources.infrastructure.utils

import de.solidblocks.core.IResourceLookup

class PassSecretLookup(val key: String) : IResourceLookup<String> {
    override fun id(): String {
        return key
    }
}
