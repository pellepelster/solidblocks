package de.solidblocks.api.resources

import de.solidblocks.core.IInfrastructureResource

object NullResource : IInfrastructureResource<Any, Any> {

    override fun id(): String {
        return "<null resource>"
    }
}
