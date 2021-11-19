package de.solidblocks.core

object NullResource : IInfrastructureResource<Any, Any> {

    override fun name(): String {
        return "<null resource>"
    }
}
