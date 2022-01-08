package de.solidblocks.api.resources

import de.solidblocks.core.IInfrastructureResource
import java.util.*

class TestResource(override val name: String, override val parents: Set<TestResource> = emptySet()) :
    ITestResourceLookup,
    IInfrastructureResource<String, String>
