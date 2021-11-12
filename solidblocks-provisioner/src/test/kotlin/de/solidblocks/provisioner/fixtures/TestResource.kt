package de.solidblocks.provisioner.fixtures

import de.solidblocks.core.IDataSource
import de.solidblocks.core.IInfrastructureResource
import java.util.*

class TestResource(val name: String, private val parents: List<TestResource> = emptyList()) :
    IInfrastructureResource<String> {

    constructor(uuid: UUID, parents: List<TestResource> = emptyList()) : this(uuid.toString(), parents)

    override fun getParents(): List<IInfrastructureResource<*>> {
        return parents
    }

    override fun name(): String {
        return name
    }

    override fun getParentDataSources(): List<IDataSource<*>> {
        return emptyList()
    }
}
