package de.solidblocks.provisioner.fixtures

import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.core.IResourceLookup
import java.util.*

class TestResource(val name: String, private val parents: List<TestResource> = emptyList()) :
        ITestResourceLookup,
        IInfrastructureResource<String, String> {

    constructor(uuid: UUID, parents: List<TestResource> = emptyList()) : this(uuid.toString(), parents)

    override fun getParents() = parents

    override fun name(): String {
        return name
    }

}
