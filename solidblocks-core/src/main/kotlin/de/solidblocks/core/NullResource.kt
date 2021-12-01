package de.solidblocks.core

object NullResource : IInfrastructureResource<Any, Any> {

    override fun id(): String {
        return "<null resource>"
    }
}
