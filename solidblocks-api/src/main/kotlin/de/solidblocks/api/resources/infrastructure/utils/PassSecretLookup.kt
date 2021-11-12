package de.solidblocks.api.resources.infrastructure.utils

import de.solidblocks.core.IDataSource

class PassSecretLookup(val key: String) : IDataSource<String> {
    override fun name(): String {
        return key
    }
}
