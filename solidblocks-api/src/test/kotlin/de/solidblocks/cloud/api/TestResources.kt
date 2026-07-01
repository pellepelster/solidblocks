package de.solidblocks.cloud.api

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup

class TestRuntime(val name: String) : BaseInfrastructureResourceRuntime()

class TestLookup(name: String) : InfrastructureResourceLookup<TestRuntime>(name, emptySet())

class TestResource(
    name: String,
    dependsOn: Set<BaseResource> = emptySet(),
    taintable: Boolean = true,
    taintRequiresRecreate: Boolean = false,
) : BaseInfrastructureResource<TestRuntime>(name, dependsOn, taintable, taintRequiresRecreate) {
    override val lookupType = TestLookup::class

    override fun asLookup() = TestLookup(name)
}

class OtherRuntime(val name: String) : BaseInfrastructureResourceRuntime()

class OtherLookup(name: String) : InfrastructureResourceLookup<OtherRuntime>(name, emptySet())

class OtherResource(
    name: String,
    dependsOn: Set<BaseResource> = emptySet(),
) : BaseInfrastructureResource<OtherRuntime>(name, dependsOn) {
    override val lookupType = OtherLookup::class

    override fun asLookup() = OtherLookup(name)
}
