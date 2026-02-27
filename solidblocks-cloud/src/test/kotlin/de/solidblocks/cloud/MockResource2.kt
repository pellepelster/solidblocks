package de.solidblocks.cloud

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.api.resources.BaseResource

class MockResource2Runtime

class MockResource2(name: String, dependsOn: Set<BaseResource> = emptySet()) : BaseInfrastructureResource<MockResource2Runtime>(name, dependsOn) {
    override val lookupType = MockResource2Lookup::class
}

class MockResource2Lookup(name: String) : InfrastructureResourceLookup<MockResource2Runtime>(name, emptySet()) {
    override fun logText() = "custom log text '$name'"
}