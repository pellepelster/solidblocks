package de.solidblocks.cloud

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.api.resources.BaseResource

class MockResource3Runtime

class MockResource3(name: String, dependsOn: Set<BaseResource> = emptySet()) : BaseInfrastructureResource<MockResource3Runtime>(name, dependsOn) {
    override val lookupType = MockResource3Lookup::class
}

class MockResource3Lookup(name: String) : InfrastructureResourceLookup<MockResource3Runtime>(name, emptySet()) {}