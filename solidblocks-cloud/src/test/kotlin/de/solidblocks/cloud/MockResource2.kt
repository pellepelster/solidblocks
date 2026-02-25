package de.solidblocks.cloud

import de.solidblocks.cloud.api.resources.InfrastructureResource
import de.solidblocks.cloud.api.resources.ResourceLookup

class MockResource2Runtime

class MockResource2(override val name: String) : InfrastructureResource<MockResource2Runtime>()

class MockResource2Lookup(override val name: String) : ResourceLookup<MockResource2Runtime>