package de.solidblocks.api.resources

import de.solidblocks.core.IInfrastructureResource
import java.util.*

class TestResource(val id: String, private val parents: Set<TestResource> = emptySet()) :
    ITestResourceLookup,
    IInfrastructureResource<String, String> {

    constructor(uuid: UUID, parents: Set<TestResource> = emptySet()) : this(uuid.toString(), parents)

    override fun getParents() = parents

    override fun id(): String {
        return id
    }
}
