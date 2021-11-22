package de.solidblocks.api.resources.infrastructure.utils

import de.solidblocks.core.IResourceLookup

class PassSecretLookup(val key: String) : IResourceLookup<String> {
    override fun name(): String {
        return key
    }
}
