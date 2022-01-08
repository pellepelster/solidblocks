package de.solidblocks.provisioner.fixtures

import de.solidblocks.core.IInfrastructureResource
import java.util.*

class TestResource(
    override val name: String,
    override val parents: Set<TestResource> = emptySet(),
    var healthy: Boolean = true,
    val hasChanges: Boolean = false
) :
    ITestResourceLookup,
    IInfrastructureResource<String, String> {

    constructor(
        uuid: UUID,
        parents: Set<TestResource> = emptySet(),
        healthy: Boolean = true,
        hasChanges: Boolean = false
    ) : this(
        uuid.toString(),
        parents,
        healthy,
        hasChanges
    )

    override val healthCheck: (() -> Boolean) = { healthy }
}
