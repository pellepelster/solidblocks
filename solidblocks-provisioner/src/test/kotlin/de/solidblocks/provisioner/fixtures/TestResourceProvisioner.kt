package de.solidblocks.provisioner.fixtures

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.ResourceDiffItem
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.core.Result
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

@Component
class TestResourceProvisioner : IInfrastructureResourceProvisioner<TestResource, String> {

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
        failOnLookup[resource.name] = true
    }

    fun lookupCount(resource: TestResource): Int {
        return lookupWasCalled.computeIfAbsent(resource.name) { AtomicInteger() }.get()
    }

    fun failOnApply(resource: TestResource) {
        failOnApply[resource.name] = true
    }

    fun diffIsMissing(resource: TestResource) {
        diffIsMissing[resource.name] = true
    }

    fun applyCount(resource: TestResource): Int {
        return applyWasCalled.computeIfAbsent(resource.name) { AtomicInteger() }.get()
    }

    fun failOnDiff(resource: TestResource) {
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

    override fun getResourceType(): Class<TestResource> {
        return TestResource::class.java
    }

    override fun lookup(resource: TestResource): Result<String> {
        lookupWasCalled.computeIfAbsent(resource.name) { AtomicInteger() }.incrementAndGet()

        if (failOnLookup.containsKey(resource.name)) {
            throw RuntimeException()
        } else {
            return Result(resource, result = "result")
        }
    }

    override fun diff(resource: TestResource): Result<ResourceDiff<String>> {
        diffWasCalled.computeIfAbsent(resource.name) { AtomicInteger() }.incrementAndGet()
        if (failOnDiff.containsKey(resource.name)) {
            throw RuntimeException()
        } else {
            val changes = ArrayList<ResourceDiffItem>()

            if (diffNeedsRecreate.containsKey(resource.name)) {
                changes.add(ResourceDiffItem("something", triggersRecreate = true))
            }

            return Result(
                resource,
                result = ResourceDiff(resource, missing = diffIsMissing.containsKey(resource.name), changes = changes)
            )
        }
    }

    override fun apply(resource: TestResource): Result<*> {
        applyWasCalled.computeIfAbsent(resource.name) { AtomicInteger() }.incrementAndGet()

        if (failOnApply.containsKey(resource.name)) {
            throw RuntimeException()
        } else {
            return Result(resource, result = "result")
        }
    }

    override fun destroy(resource: TestResource): Result<*> {
        destroyWasCalled.computeIfAbsent(resource.name) { AtomicInteger() }.incrementAndGet()

        if (failOnDestroy.containsKey(resource.name)) {
            throw RuntimeException()
        } else {
            return Result(resource, result = "result")
        }
    }

    override fun destroyAll(): Result<*> {
        throw RuntimeException()
    }
}
