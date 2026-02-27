package de.solidblocks.cloud

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup

class MockResource1Runtime

class MockResource1(name: String) : BaseInfrastructureResource<MockResource1Runtime>(name, emptySet()) {
    override val lookupType = MockResource1Lookup::class
}

class MockResource1Lookup(name: String) : InfrastructureResourceLookup<MockResource1Runtime>(name, emptySet()) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MockResource1Lookup

        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}