package de.solidblocks.cloud

import de.solidblocks.cloud.api.resources.InfrastructureResource
import de.solidblocks.cloud.api.resources.ResourceLookup

class MockResource1Runtime

class MockResource1(override val name: String) : InfrastructureResource<MockResource1Runtime>()

class MockResource1Lookup(override val name: String) : ResourceLookup<MockResource1Runtime>