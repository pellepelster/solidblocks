package de.solidblocks.api.resources.infrastructure.utils

import de.solidblocks.core.IInfrastructureResource

class PassSecret(val key: String, val value: String) : IInfrastructureResource<PassSecret, String> {
    override fun id(): String {
        return key
    }
}
