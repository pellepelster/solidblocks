package de.solidblocks.provisioner.fixtures

import de.solidblocks.core.IInfrastructureResource
import java.util.*

class TestResource(val id: String, private val parents: List<TestResource> = emptyList()) :
    ITestResourceLookup,
    IInfrastructureResource<String, String> {

    constructor(uuid: UUID, parents: List<TestResource> = emptyList()) : this(uuid.toString(), parents)

    override fun getParents() = parents

    override fun id(): String {
        return id
    }
}
