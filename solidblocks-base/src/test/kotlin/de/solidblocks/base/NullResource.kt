package de.solidblocks.base

import de.solidblocks.core.IInfrastructureResource
import java.util.*

object NullResource : IInfrastructureResource<Any, Any> {
    override val name = "null-resource-${UUID.randomUUID()}"
}
