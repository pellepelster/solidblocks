package de.solidblocks.provisioner.fixtures

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.ResourceDiffItem
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.Result
import java.util.concurrent.atomic.AtomicInteger

class TestResourceProvisioner : IResourceLookupProvider<ITestResourceLookup, String>, IInfrastructureResourceProvisioner<TestResource, String> {

    val failOnDiff = HashMap<String, Boolean>()
    var diffWasCalled = HashMap<String, AtomicInteger>()

    var failOnLookup = HashMap<String, Boolean>()
    var lookupWasCalled = HashMap<String, AtomicInteger>()

    var failOnApply = HashMap<String, Boolean>()
    var applyWasCalled = HashMap<String, AtomicInteger>()

    var failOnDestroy = HashMap<String, Boolean>()
    var destroyWasCalled = HashMap<String, AtomicInteger>()

    var diffIsMissing = HashMap<String, Boolean>()
    var diffNeedsRecreate = HashMap<String, AtomicInteger>()

    fun failOnLookup(resource: TestResource) {
        failOnLookup[resource.id] = true
    }

    fun lookupCount(resource: TestResource): Int {
        return lookupWasCalled.computeIfAbsent(resource.id) { AtomicInteger() }.get()
    }

    fun failOnApply(resource: TestResource) {
        failOnApply[resource.id] = true
    }

    fun diffIsMissing(resource: TestResource) {
        diffIsMissing[resource.id] = true
    }

    fun applyCount(resource: TestResource): Int {
        return applyWasCalled.computeIfAbsent(resource.id) { AtomicInteger() }.get()
    }

    fun failOnDiff(resource: TestResource) {
        failOnDiff[resource.id] = true
    }

    fun diffCount(resource: TestResource): Int {
        return diffWasCalled.computeIfAbsent(resource.id) { AtomicInteger() }.get()
    }

    fun reset() {
        failOnDiff.clear()
        diffWasCalled.clear()

        failOnLookup.clear()
        lookupWasCalled.clear()

        failOnApply.clear()
        applyWasCalled.clear()

        failOnDestroy.clear()
        destroyWasCalled.clear()

        diffIsMissing.clear()
        diffNeedsRecreate.clear()
    }

    override fun getResourceType(): Class<TestResource> {
        return TestResource::class.java
    }

    override fun diff(resource: TestResource): Result<ResourceDiff> {
        diffWasCalled.computeIfAbsent(resource.id) { AtomicInteger() }.incrementAndGet()
        if (failOnDiff.containsKey(resource.id)) {
            throw RuntimeException()
        } else {
            val changes = ArrayList<ResourceDiffItem>()

            if (diffNeedsRecreate.containsKey(resource.id)) {
                changes.add(ResourceDiffItem("something", triggersRecreate = true))
            }

            return Result(
                result = ResourceDiff(resource, missing = diffIsMissing.containsKey(resource.id), changes = changes)
            )
        }
    }

    override fun apply(resource: TestResource): Result<*> {
        applyWasCalled.computeIfAbsent(resource.id) { AtomicInteger() }.incrementAndGet()

        if (failOnApply.containsKey(resource.id)) {
            throw RuntimeException()
        } else {
            return Result(result = "result")
        }
    }

    override fun destroy(resource: TestResource): Boolean {
        destroyWasCalled.computeIfAbsent(resource.id) { AtomicInteger() }.incrementAndGet()

        if (failOnDestroy.containsKey(resource.id)) {
            throw RuntimeException()
        } else {
            return true
        }
    }

    override fun lookup(lookup: ITestResourceLookup): Result<String> {
        lookupWasCalled.computeIfAbsent(lookup.id()) { AtomicInteger() }.incrementAndGet()

        if (failOnLookup.containsKey(lookup.id())) {
            throw RuntimeException()
        } else {
            return Result(result = "result")
        }
    }

    override fun getLookupType(): Class<*> {
        return ITestResourceLookup::class.java
    }
}
