package de.solidblocks.provisioner.fixtures

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.ResourceDiffItem
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.Result
import java.util.concurrent.atomic.AtomicInteger

class TestResourceProvisioner :
    IResourceLookupProvider<ITestResourceLookup, String>,
    IInfrastructureResourceProvisioner<TestResource, String> {

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

    fun mockFailOnLookupFor(resource: TestResource) {
        failOnLookup[resource.name] = true
    }

    fun lookupCount(resource: TestResource): Int {
        return lookupWasCalled.computeIfAbsent(resource.name) { AtomicInteger() }.get()
    }

    fun mockFailOnApplyFor(resource: TestResource) {
        failOnApply[resource.name] = true
    }

    fun mockMissingDiffFor(resource: TestResource) {
        diffIsMissing[resource.name] = true
    }

    fun applyCount(resource: TestResource): Int {
        return applyWasCalled.computeIfAbsent(resource.name) { AtomicInteger() }.get()
    }

    fun noInteractions(resource: TestResource): Boolean {
        return applyWasCalled.computeIfAbsent(resource.name) { AtomicInteger() }
            .get() + destroyWasCalled.computeIfAbsent(
            resource.name
        ) { AtomicInteger() }.get() + diffWasCalled.computeIfAbsent(resource.name) { AtomicInteger() }.get() == 0
    }

    fun mockFailOnDiffFor(resource: TestResource) {
        failOnDiff[resource.name] = true
    }

    fun diffCount(resource: TestResource): Int {
        return diffWasCalled.computeIfAbsent(resource.name) { AtomicInteger() }.get()
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

    override fun diff(resource: TestResource): Result<ResourceDiff> {
        diffWasCalled.computeIfAbsent(resource.name) { AtomicInteger() }.incrementAndGet()
        if (failOnDiff.containsKey(resource.name)) {
            throw RuntimeException()
        } else {
            val changes = ArrayList<ResourceDiffItem>()

            if (resource.hasChanges) {
                changes.add(ResourceDiffItem("something", changed = true))
            }

            if (diffNeedsRecreate.containsKey(resource.name)) {
                changes.add(ResourceDiffItem("something", triggersRecreate = true))
            }

            return Result(
                result = ResourceDiff(resource, missing = diffIsMissing.containsKey(resource.name), changes = changes)
            )
        }
    }

    override fun apply(resource: TestResource): Result<*> {
        applyWasCalled.computeIfAbsent(resource.name) { AtomicInteger() }.incrementAndGet()

        if (failOnApply.containsKey(resource.name)) {
            throw RuntimeException()
        } else {
            return Result(result = "result")
        }
    }

    override fun destroy(resource: TestResource): Boolean {
        destroyWasCalled.computeIfAbsent(resource.name) { AtomicInteger() }.incrementAndGet()

        if (failOnDestroy.containsKey(resource.name)) {
            throw RuntimeException()
        } else {
            return true
        }
    }

    override fun lookup(lookup: ITestResourceLookup): Result<String> {
        lookupWasCalled.computeIfAbsent(lookup.name) { AtomicInteger() }.incrementAndGet()

        if (failOnLookup.containsKey(lookup.name)) {
            throw RuntimeException()
        } else {
            return Result(result = "result")
        }
    }

    override val resourceType = TestResource::class.java

    override val lookupType = ITestResourceLookup::class.java
}
